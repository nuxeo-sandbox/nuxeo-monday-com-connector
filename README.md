# Description
A plugin that provides an integration between [monday.com](https://monday.com/) and the Nuxeo Platform.

# How to build
```bash
git clone https://github.com/nuxeo-sandbox/nuxeo-monday-com-connector
cd nuxeo-monday-com-connector
mvn clean install
```

# Plugin Features
## Data Model
The plugin includes a `monday` schema that contains that can be used to link a Nuxeo Document to Monday.com object.  
The schema is typically added to a Document in Nuxeo using the `Monday` Facet.

## Webhooks
This plugin implements an endpoint to receive [webhook events from monday.com](https://monday.com/integrations/webhooks). The callback URL is `https://myserver/nuxeo/site/monday/event`. The endpoint will generate a `mondayEvent` event in Nuxeo that can be catched with event listeners in java or event handlers in Nuxeo Studio. 
Below is a sample automation script triggered by event handler that creates a workspace when a new row is created in monday.com and sends the workspace link back to monday.com 

```js
function run(input, params) {

    Auth.LoginAs(input, {});

    var event = JSON.parse(ctx.Event.getContext().getProperty("mondayEvent")).event;

    Console.log('Received Event from Monday: '+ JSON.stringify(event, null, 2));

    if (event.type === 'create_pulse') {
        createProject(input,event);
    }

    Auth.Logout(input, {});

}

function createProject(input, event) {

    var root = Repository.GetDocument(input, {
        'value': '/default-domain/workspaces'
    });

    var project = Document.Create(root, {
        'type': 'Workspace',
        'name': ''+event.pulseId,
        'properties': {
            'dc:title':event.pulseName
        }
    });

    project = Document.AddFacet(project, {
        'facet': 'Monday',
        'save': false
    });

    project = Document.Update(project, {
        'properties': {
            'monday:boardId': ''+event.boardId,
            'monday:pulseId': ''+event.pulseId
        },
        'save': true
    });

    setNuxeoLink(project, event.boardId, event.pulseId);

    return project;
}

function setNuxeoLink(project, boardId, pulseId) {

    var mutation = "mutation {change_column_value(item_id: "+pulseId+", board_id: "+boardId+", column_id: \"link\", value: \"{\\\"url\\\":\\\"https://host/nuxeo/ui/#!/doc/"+project.id+"\\\",\\\"text\\\":\\\"Open in Nuxeo\\\"}\"){id}}";

    var query = {
        query: mutation
    };

    Console.log(JSON.stringify(query));

    var response = HTTP.post('https://api.monday.com/v2/',JSON.stringify(query),getHeaders());

    Console.log(response.getString());
}

function getHeaders() {
    return {
        headers: {
            Authorization: Env['monday.com.api.key'],
            'Content-Type': 'application/json'
        }
    };
}
```

## Monday.com GraphQL API
[Monday.com GraphQL API](https://developer.monday.com/api-reference/docs) can simply be called using the HTTP helper in Automation scripting. Below is an example of getting a user's information.

```js
function getMondayUsers(userIds) {
  
  var query = {
    query: "{users (ids: ["+userIds.join(",")+"]){id email}}"
  };
  
  Console.log(JSON.stringify(query));
  
  var response = JSON.parse(HTTP.post('https://api.monday.com/v2/',JSON.stringify(query),getHeaders()).getString());
  
  Console.log(response);
  
  return response.data.users;
}


function getHeaders() {
  return {
    headers: {
      Authorization: Env['monday.com.api.key'],
      'Content-Type': 'application/json'
    }
  };
}
```

# Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

# Nuxeo Marketplace
This plugin is published on the [marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-monday-com-connector)

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com).
