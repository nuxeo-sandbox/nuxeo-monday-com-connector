/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.labs.monday.com.webhook;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.labs.monday.com.webhook.endpoint.MondayWebhookEndpoint.CHALLENGE_FIELD;
import static org.nuxeo.labs.monday.com.webhook.endpoint.MondayWebhookEndpoint.MONDAY_EVENT;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.http.test.CloseableHttpResponse;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

@RunWith(FeaturesRunner.class)
@Features({ WebEngineFeature.class, CoreFeature.class })
@Deploy("nuxeo-monday-com-connector-webhook")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestMondayWebhookEndpoint {

    private static final String CONTENT_TYPE = "application/json";

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.builder()
            .url(() -> servletContainerFeature.getHttpUrl() + "/monday/event")
            .build();

    @Test
    public void shouldRespondToChallenge() {
        final String challenge = "abcd";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(CHALLENGE_FIELD, challenge);
        String jsonPost = jsonObject.toString();

        try (CloseableHttpResponse response = httpClient.buildPostRequest("").contentType(CONTENT_TYPE).entity(jsonPost).execute()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JSONTokener tokener = new JSONTokener(response.getEntityInputStream());
            JSONObject jsonResponse = new JSONObject(tokener);
            Assert.assertTrue(jsonResponse.has(CHALLENGE_FIELD));
            Assert.assertEquals(challenge,jsonResponse.getString(CHALLENGE_FIELD));
        }
    }


    @Test
    public void shouldFireEvent() throws IOException {
        try (CapturingEventListener listener = new CapturingEventListener(MONDAY_EVENT)) {
            File jsonPayload = FileUtils.getResourceFileFromContext("files/sample_event.json");
            byte[] jsonData = Files.readAllBytes(Paths.get(jsonPayload.toURI()));
            String jsonPost = new String(jsonData, StandardCharsets.UTF_8);

            try (CloseableHttpResponse response = httpClient.buildPostRequest("").contentType(CONTENT_TYPE).entity(jsonPost).execute()) {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                assertEquals(1, listener.getCapturedEventCount(MONDAY_EVENT));
            }
        }
    }

    @Test
    public void noEventIsBadRequest() {
        try (CloseableHttpResponse response = httpClient.buildPostRequest("").contentType(CONTENT_TYPE).entity("{}").execute()) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    protected String getBaseURL() {
        int port = servletContainerFeature.getPort();
        return "http://localhost:" + port;
    }

}
