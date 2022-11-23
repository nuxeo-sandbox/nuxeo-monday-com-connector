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

package org.nuxeo.labs.monday.com.webhook.endpoint;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nuxeo.runtime.api.Framework;

import static java.util.Collections.singletonMap;

/**
 * WebEngine module to handle the monday.com webhooks
 */
@Path("/monday")
@WebObject(type = "monday")
@Consumes({ MediaType.APPLICATION_JSON })
public class MondayWebhookEndpoint extends ModuleRoot {

    protected static final Log log = LogFactory.getLog(MondayWebhookEndpoint.class);

    public static final String MONDAY_EVENT = "mondayEvent";

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String CHALLENGE_FIELD = "challenge";

    public static final String EVENT_FIELD = "event";

    @Path("/event")
    @POST
    public Object doPost(@Context HttpServletRequest request) {
        try {
            String requestBody = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
            JsonNode eventJson = objectMapper.readTree(requestBody);

            if (eventJson.has(CHALLENGE_FIELD)) {
                // handle challenge
                String challenge = eventJson.get(CHALLENGE_FIELD).asText();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(CHALLENGE_FIELD, challenge);
                return new StringBlob(jsonObject.toString(), MediaType.APPLICATION_JSON);
            } else if (eventJson.has(EVENT_FIELD)) {
                // fire event
                EventContextImpl ctx = new EventContextImpl();
                Map<String, Serializable> props = singletonMap(MONDAY_EVENT, requestBody);
                ctx.setProperties(props);
                EventService es = Framework.getService(EventService.class);
                es.fireEvent(MONDAY_EVENT, ctx);
                return Response.status(Response.Status.OK).build();
            } else {
                log.error("Error processing the event");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            // Handle errors and send correct server responses
        } catch (JsonProcessingException e) {
            log.error("Error processing the event", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing the event", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
