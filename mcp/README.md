# RUN PROJECT:
### PACKAGING:
````
mvn clean install
````
### RUN:
````agsl
java -jar target/mcp-0.0.1-SNAPSHOT.jar
````
# RUN MCP INSPECTOR:
```
npx @modelcontextprotocol/inspector
```
* Transport Type: Select SSE
* URL: http://localhost:8080/sse
* Click : connect

# CREATE NEW COLLECTION IN Qdrant:
run in  qdrant console:
````
PUT collections/Jira2
{
    "vectors": {
        "size": 768, //embedding size
        "distance": "Cosine"
    }
}
````