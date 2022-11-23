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

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

@RunWith(FeaturesRunner.class)
@Features({ WebEngineFeature.class, CoreFeature.class })
@Deploy("nuxeo-monday-com-connector-webhook")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestMondayWebhookEndpoint {

    private static final String CONTENT_TYPE = "application/json";

    private static final Integer TIMEOUT = 1000 * 10; // 10s

    protected Client client;

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Before
    public void setup() {
        client = Client.create();
        client.setConnectTimeout(TIMEOUT);
        client.setReadTimeout(TIMEOUT);
        client.setFollowRedirects(Boolean.FALSE);
    }

    @Test
    public void shouldRespondToChallenge() {
        final String challenge = "abcd";

        WebResource webResource = getWebhookResource();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(CHALLENGE_FIELD, challenge);
        String jsonPost = jsonObject.toString();

        ClientResponse response = webResource.accept(CONTENT_TYPE)
                .type(CONTENT_TYPE)
                .post(ClientResponse.class, jsonPost);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JSONTokener tokener = new JSONTokener(response.getEntityInputStream());
        JSONObject jsonResponse = new JSONObject(tokener);
        Assert.assertTrue(jsonResponse.has(CHALLENGE_FIELD));
        Assert.assertEquals(challenge,jsonResponse.getString(CHALLENGE_FIELD));
    }


    @Test
    public void shouldFireEvent() throws IOException {

        CapturingEventListener listener = new CapturingEventListener(MONDAY_EVENT);

        WebResource webResource = getWebhookResource();

        File jsonPayload = FileUtils.getResourceFileFromContext("files/sample_event.json");
        byte[] jsonData = Files.readAllBytes(Paths.get(jsonPayload.toURI()));
        String jsonPost = new String(jsonData, StandardCharsets.UTF_8);

        ClientResponse response = webResource.accept(CONTENT_TYPE)
                                             .type(CONTENT_TYPE)
                                             .post(ClientResponse.class, jsonPost);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(1, listener.getCapturedEventCount(MONDAY_EVENT));
    }

    @Test
    public void noEventIsBadRequest() {
        WebResource webResource = getWebhookResource();
        ClientResponse response = webResource.accept(CONTENT_TYPE)
                .type(CONTENT_TYPE)
                .post(ClientResponse.class, "{}");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    protected String getBaseURL() {
        int port = servletContainerFeature.getPort();
        return "http://localhost:" + port;
    }

    protected WebResource getWebhookResource() {
        return  client.resource(getBaseURL()).path("monday").path("event");
    }

}
