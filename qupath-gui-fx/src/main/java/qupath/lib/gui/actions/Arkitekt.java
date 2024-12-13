package qupath.lib.gui.actions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;




class Requirement {
    public String service;
    public String key;

    public Requirement(String service, String key) {
        this.service = service;
        this.key = key;
    }
}


class Manifest {
    public String identifier;
    public String version = "1.0";
    public List<String> scopes = List.of("read");
    public List<Requirement> requirements;

    public Manifest(String identifier, List<Requirement>  requirements) {
        this.identifier = identifier;
        this.requirements = requirements;
    }
}


class FaktsStartRequest {
    public Manifest manifest;
    public String requested_client_kind;

    public FaktsStartRequest(Manifest manifest, String requested_client_kind) {
        this.manifest = manifest;
        this.requested_client_kind = requested_client_kind;
    }
}


class DeviceCodeAnswer {
    public String code;

    public DeviceCodeAnswer(String code) {
        this.code = code;
    }
}


class DeviceCodeChallenge {
    public String code;

    public DeviceCodeChallenge(String code) {
        this.code = code;
    }
}

class ChallengeAnswer {
    public String status;
    @Nullable public String token;

    public ChallengeAnswer(String status, @Nullable String token) {
        this.status = status;
        this.token = token;
    }
}


class RetrieveRequest {
    public String token;

    public RetrieveRequest(String token) {
        this.token = token;
    }
}


class UnlokFakt {
    public String client_id;
    public String client_secret;
    public List<String> scopes;
}

class Fakts {
    public UnlokFakt unlok;


    public Fakts(UnlokFakt unlok) {
        this.unlok = unlok;
    }
}


class RetrieveAnswer {
    public Fakts config;

    public RetrieveAnswer(Fakts config) {
        this.config = config;
    }
}


class TokenResponse {
    public String access_token;
    public String token_type;
    public String scope;
    public String expires_in;

    public TokenResponse(String access_token, String token_type, String scope, String expires_in) {
        this.access_token = access_token;
        this.token_type = token_type;
        this.scope = scope;
        this.expires_in = expires_in;
    }

   
}



public class Arkitekt {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Manifest manifest = new Manifest("qupath", List.of(new Requirement("live.arkitekt.lok", "unlok")));

    public  Arkitekt() {



    }

    public String loginUser(UnlokFakt unlok) throws Exception {
        String tokenUrl = "http://127.0.0.1/lok/o/token/";
        String clientId = unlok.client_id;
        String clientSecret =  unlok.client_secret;
        String scope = String.join(" ", unlok.scopes);
        String bodyString = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
        RequestBody body = RequestBody.create(bodyString, MediaType.get("application/x-www-form-urlencoded; charset=utf-8"));

        // Create request
        Request request = new Request.Builder()
                .url(tokenUrl)
                .post(body)
                .build();

        // Execute request
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // Handle response
                String responseJson = response.body().string();
                System.out.println("Response: " + responseJson);

                TokenResponse tokenResponse = gson.fromJson(responseJson, TokenResponse.class);

                System.out.println("Token Response: " + tokenResponse);
                return tokenResponse.access_token;
                // Parse the token from the response if needed
            } else {
                throw new Exception("Failed to obtain token. Response code: " + response.code());
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred why login in: " + e.getMessage(), e);
        }
        
    }


    public Fakts retrieveFakts(String token) throws Exception {

        // Create JSON from challenge
        String challengeJson = gson.toJson(new RetrieveRequest(token));

        // Create request body
        RequestBody challengeBody = RequestBody.create(challengeJson, MediaType.get("application/json; charset=utf-8"));

        // Create request
        Request challengeRequest = new Request.Builder()
                .url("http://127.0.0.1/lok/f/claim/")
                .post(challengeBody)
                .build();

        // Execute request
        try (Response challengeResponse = client.newCall(challengeRequest).execute()) {
            if (challengeResponse.isSuccessful() && challengeResponse.body() != null) {
                // Handle successful challenge response
                String answerJson = challengeResponse.body().string();
                RetrieveAnswer answer = gson.fromJson(answerJson, RetrieveAnswer.class);

                return answer.config;
            } else {
                throw new Exception("Failed to retrieve Fakts. Response code: " + challengeResponse.code());
            }
        }

    }

  
    public void login(String url) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                // Create JSON from manifest
                String json = gson.toJson(new FaktsStartRequest(manifest, "development"));

                System.out.println("JSON Request: " + json);

                // Create request body
                RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

                // Create request
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                // Execute request
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Handle response (e.g., display device code)
                        DeviceCodeAnswer responseJson = gson.fromJson(response.body().string(), DeviceCodeAnswer.class);
                        String deviceCode = responseJson.code;
                        Platform.runLater(() -> {
                            

                            // Open the default web browser with the device code URL
                            try {

                                String starturl = "http://127.0.0.1/lok/f/configure/?grant=device_code&device_code=" + deviceCode;
                                System.err.println(starturl);
                                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

                                java.net.URI oURL = new java.net.URI(starturl);
                                desktop.browse(oURL);
                            } catch (Exception e) {
                                // Display device code (e.g., in an alert)
                                System.out.println("Couldn't open webbrowser: " + e.getMessage());

                                Alert linkAlert = new Alert(Alert.AlertType.INFORMATION);
                                linkAlert.setTitle("Device Code");
                                linkAlert.setHeaderText(null);
                                linkAlert.setContentText("Please click the following link to authenticate:\nhttp://127.0.0.1/f/lok/device?code=" + deviceCode);
                                linkAlert.showAndWait();
                            }
                        });

                        // Challenge the web server in another thread
                        executor.submit(() -> {
                            try {
                                for (int i = 0; i < 10; i++) {
                                    // Wait for the user to authenticate
                                    Thread.sleep(2000);

                                    // Create JSON from challenge
                                    String challengeJson = gson.toJson(new DeviceCodeChallenge(deviceCode));

                                    // Create request body
                                    RequestBody challengeBody = RequestBody.create(challengeJson, MediaType.get("application/json; charset=utf-8"));

                                    // Create request
                                    Request challengeRequest = new Request.Builder()
                                            .url("http://127.0.0.1/lok/f/challenge/")
                                            .post(challengeBody)
                                            .build();

                                    // Execute request
                                    try (Response challengeResponse = client.newCall(challengeRequest).execute()) {
                                        if (challengeResponse.isSuccessful() && challengeResponse.body() != null) {
                                            // Handle successful challenge response
                                            String answerJson = challengeResponse.body().string();
                                            ChallengeAnswer answer = gson.fromJson(answerJson, ChallengeAnswer.class);

                                            if (!answer.status.equals("granted")) {
                                                // Continue waiting
                                                System.out.println("Challenge pending");
                                            } else {

                                                Fakts fakts = retrieveFakts(answer.token);

                                                String token = loginUser(fakts.unlok);

                                                // Handle approved response
                                                Platform.runLater(() -> {
                                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                                    alert.setTitle("Error");
                                                    alert.setHeaderText(null);
                                                    alert.setContentText("Successfully challenged. Token: " + token);
                                                    alert.showAndWait();
                                                });

                                                System.out.println("Challenge successful");
                                                break; // Exit loop if successful
                                            }
                                        } else {
                                            // Handle error response
                                            Platform.runLater(() -> {
                                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                                alert.setTitle("Error");
                                                alert.setHeaderText(null);
                                                alert.setContentText("Failed to challenge. Response code: " + challengeResponse.code());
                                                alert.showAndWait();
                                            });
                                        }
                                    }
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Error");
                                    alert.setHeaderText(null);
                                    alert.setContentText("Thread was interrupted: " + e.getMessage());
                                    alert.showAndWait();
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Error");
                                    alert.setHeaderText(null);
                                    alert.setContentText("An error occurred: " + e.getMessage());
                                    alert.showAndWait();
                                });
                            }
                        });
                     
                    } else {
                        // Handle error response
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText(null);
                            alert.setContentText("Failed to send manifest. Response code: " + response.code());
                            alert.showAndWait();
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("An error occurred: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }
}
