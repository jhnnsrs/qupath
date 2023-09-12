package qupath.lib.images.servers.openslide;

/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2011 Carnegie Mellon University
 *  All rights reserved.
 *
 *  OpenSlide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, version 2.1.
 *
 *  OpenSlide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with OpenSlide. If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 */

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public final class OpenSlide implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(OpenSlide.class);
    private static final String LIBRARY_VERSION = OpenSlideJNA.INSTANCE.openslide_get_version();

    public static final String PROPERTY_NAME_COMMENT = "openslide.comment";
    public static final String PROPERTY_NAME_VENDOR = "openslide.vendor";
    public static final String PROPERTY_NAME_QUICKHASH1 = "openslide.quickhash-1";
    public static final String PROPERTY_NAME_BACKGROUND_COLOR = "openslide.background-color";
    public static final String PROPERTY_NAME_OBJECTIVE_POWER = "openslide.objective-power";
    public static final String PROPERTY_NAME_MPP_X = "openslide.mpp-x";
    public static final String PROPERTY_NAME_MPP_Y = "openslide.mpp-y";
    public static final String PROPERTY_NAME_BOUNDS_X = "openslide.bounds-x";
    public static final String PROPERTY_NAME_BOUNDS_Y = "openslide.bounds-y";
    public static final String PROPERTY_NAME_BOUNDS_WIDTH = "openslide.bounds-width";
    public static final String PROPERTY_NAME_BOUNDS_HEIGHT = "openslide.bounds-height";

    public List<String> getAssociatedImages() {
        return associatedImages;
    }

    private final List<String> associatedImages;

    private long osr;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final long[] levelWidths;
    private final long[] levelHeights;
    private final int levelCount;
    private final Map<String, String> properties;
    private final File canonicalFile;
    private final int hashCodeVal;

    public static String detectVendor(File file) {
        return OpenSlideJNA.INSTANCE.openslide_detect_vendor(file.getPath());
    }

    public OpenSlide(String file) throws IOException {
        File f = new File (file);
        if (!f.exists()) {
            throw new FileNotFoundException(file);
        }

        osr = OpenSlideJNA.INSTANCE.openslide_open(file);

        if (osr == 0) {
            throw new IOException(file + ": Not a file that OpenSlide can recognize");
        }
        // dispose on error, we are in the constructor
        try {
            checkError();
        } catch (IOException e) {
            dispose();
            throw e;
        }

        // store level count
        levelCount = OpenSlideJNA.INSTANCE.openslide_get_level_count(osr);

        // store dimensions
        levelWidths = new long[levelCount];
        levelHeights = new long[levelCount];

        for (int i = 0; i < levelCount; i++) {
            long[] w = new long[1], h = new long[1];
            OpenSlideJNA.INSTANCE.openslide_get_level_dimensions(osr, i, w, h);
            levelWidths[i] = w[0];
            levelHeights[i] = h[0];
        }

        // properties
        Map<String, String> props = new LinkedHashMap<>();
        for (String s : OpenSlideJNA.INSTANCE.openslide_get_property_names(osr)) {
            props.put(s, OpenSlideJNA.INSTANCE.openslide_get_property_value(osr, s));
        }

        properties = Collections.unmodifiableMap(props);

        // associated images
        associatedImages = new ArrayList<>();
        Collections.addAll(associatedImages, OpenSlideJNA.INSTANCE
                .openslide_get_associated_image_names(osr));


        // store info for hash and equals
        canonicalFile = f.getCanonicalFile();
        String quickhash1 = getProperties().get(PROPERTY_NAME_QUICKHASH1);
        if (quickhash1 != null) {
            hashCodeVal = (int) Long.parseLong(quickhash1.substring(0, 8), 16);
        } else {
            hashCodeVal = canonicalFile.hashCode();
        }

        // dispose on error, we are in the constructor
        try {
            checkError();
        } catch (IOException e) {
            dispose();
            throw e;
        }
    }

    // call with the reader lock held, or from the constructor
    private void checkError() throws IOException {
        String msg = OpenSlideJNA.INSTANCE.openslide_get_error(osr);

        if (msg != null) {
            throw new IOException(msg);
        }
    }

    // takes the writer lock
    public void dispose() {
        Lock wl = lock.writeLock();
        wl.lock();
        try {
            if (osr != 0) {
                OpenSlideJNA.INSTANCE.openslide_close(osr);
                osr = 0;
            }
        } finally {
            wl.unlock();
        }
    }

    public int getLevelCount() {
        return levelCount;
    }

    // call with the reader lock held
    private void checkDisposed() {
        if (osr == 0) {
            throw new OpenSlideDisposedException();
        }
    }

    public long getLevel0Width() {
        return levelWidths[0];
    }

    public long getLevel0Height() {
        return levelHeights[0];
    }

    public long getLevelWidth(int level) {
        return levelWidths[level];
    }

    public long getLevelHeight(int level) {
        return levelHeights[level];
    }

    // takes the reader lock
    public void paintRegionARGB(int[] dest, long x, long y, int level, int w,
                                int h) throws IOException {
        if ((long) w * (long) h > dest.length) {
            throw new ArrayIndexOutOfBoundsException("Size of data ("
                    + dest.length + ") is less than w * h");
        }

        if (w < 0 || h < 0) {
            throw new IllegalArgumentException("w and h must be nonnegative");
        }

        Lock rl = lock.readLock();
        rl.lock();
        try {
            checkDisposed();
            OpenSlideJNA.INSTANCE.openslide_read_region(osr, dest, x, y, level, w, h);
            checkError();
        } finally {
            rl.unlock();
        }
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    // takes the reader lock
    BufferedImage getAssociatedImage(String name) throws IOException {
        Lock rl = lock.readLock();
        rl.lock();
        try {
            checkDisposed();

            long[]d0 = new long[1];
            long[]d1 = new long[1];
            OpenSlideJNA.INSTANCE.openslide_get_associated_image_dimensions(osr, name,
                    d0, d1);
            checkError();
            if (d0[0] == -1) {
                // non-terminal error
                throw new IOException("Failure reading associated image");
            }

            BufferedImage img = new BufferedImage((int) d0[0], (int) d1[0],
                    BufferedImage.TYPE_INT_ARGB_PRE);

            int[] data = ((DataBufferInt) img.getRaster().getDataBuffer())
                    .getData();

            OpenSlideJNA.INSTANCE.openslide_read_associated_image(osr, name, data);
            checkError();
            return img;
        } finally {
            rl.unlock();
        }
    }

    public static String getLibraryVersion() {
        return LIBRARY_VERSION;
    }

    @Override
    public int hashCode() {
        return hashCodeVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof OpenSlide os2) {
            String quickhash1 = getProperties()
                    .get(PROPERTY_NAME_QUICKHASH1);
            String os2_quickhash1 = os2.getProperties()
                    .get(PROPERTY_NAME_QUICKHASH1);

            if (quickhash1 != null && os2_quickhash1 != null) {
                return quickhash1.equals(os2_quickhash1);
            } else if (quickhash1 == null && os2_quickhash1 == null) {
                return canonicalFile.equals(os2.canonicalFile);
            } else {
                return false;
            }
        }

        return false;
    }

    @Override
    public void close() {
        dispose();
    }

    public interface OpenSlideJNA extends Library {
        OpenSlideJNA INSTANCE = Native.load("openslide", OpenSlideJNA.class);

        String openslide_get_version();
        String openslide_detect_vendor(String file);
        long openslide_open(String file);
        int openslide_get_level_count(long osr);
        void openslide_get_level_dimensions(long osr, int level, long[] w, long[] h);
        double openslide_get_level_downsample(long osr, int level);
        void openslide_close(long osr);
        String[] openslide_get_property_names(long osr);
        String openslide_get_property_value(long osr, String name);
        String[] openslide_get_associated_image_names(long osr);
        void openslide_read_region(long osr, int[] dest, long x, long y, int level, long w, long h);
        void openslide_get_associated_image_dimensions(long osr, String name, long[] w, long[] h);
        void openslide_read_associated_image(long osr, String name, int[] dest);
        String openslide_get_error(long osr);
    }

    public static class OpenSlideDisposedException extends RuntimeException {
        private static final String MSG = "OpenSlide object has been disposed";

        public OpenSlideDisposedException() {
            super(MSG);
        }
    }

}
