# Architecture:

<img width="1658" height="966" alt="image" src="https://github.com/user-attachments/assets/80aed130-f460-44d4-a77b-328fc8912bbf" />

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
## Resources  for Test:
We can use this list and insert it directly in the input field **tiketJson** in MCP Inspector to 5 elements:
````
[
  {
    "ticketKey": "GDIPROD-3751",
    "chunkId": "GDIPROD-3751__summary__0",
    "sourceField": "summary",
    "created": "1222-02-21T00:00:00Z",
    "project": "GDIPROD",
    "status": "Fermé",
    "llmCause": "Absence de gestion du contrôle de validation sur le fichier GTATDP10",
    "llmSolution": "Supprimer l'ATD créé si celui-ci n'existait pas lors de l'arrivée sur le traitement en modifiant le G2GCOR02 et le G2GCOR06"
  },
  {
    "ticketKey": "GDIPROD-3752",
    "chunkId": "GDIPROD-3752__summary__0",
    "sourceField": "summary",
    "created": "2023-03-15T00:00:00Z",
    "project": "GDIPROD",
    "status": "Fermé",
    "llmCause": "Erreur de format dans le champ montant pour le fichier GTATDP11",
    "llmSolution": "Ajouter une validation de format numérique avec gestion d'exception dans le module G2GCOR03"
  },
  {
    "ticketKey": "GDIPROD-3753",
    "chunkId": "GDIPROD-3753__summary__0",
    "sourceField": "summary",
    "created": "2023-04-22T00:00:00Z",
    "project": "GDIPROD",
    "status": "Fermé",
    "llmCause": "Blocage du traitement batch dû à un timeout sur la base de données",
    "llmSolution": "Optimiser la requête SQL dans G2GCOR07 et augmenter le timeout de connexion à 300 secondes"
  },
  {
    "ticketKey": "GDIPROD-3754",
    "chunkId": "GDIPROD-3754__summary__0",
    "sourceField": "summary",
    "created": "2023-05-10T00:00:00Z",
    "project": "GDIPROD",
    "status": "Fermé",
    "llmCause": "Doublon non détecté lors de l'insertion des enregistrements ATD",
    "llmSolution": "Implémenter un contrôle d'unicité sur la clé composite (ID_CLIENT, DATE_TRAIT) dans G2GCOR04"
  },
  {
    "ticketKey": "GDIPROD-3755",
    "chunkId": "GDIPROD-3755__summary__0",
    "sourceField": "summary",
    "created": "2023-06-18T00:00:00Z",
    "project": "GDIPROD",
    "status": "Fermé",
    "llmCause": "Fichier d'entrée GTATDP12 rejeté à cause d'un en-tête manquant",
    "llmSolution": "Ajouter une vérification d'existence et de conformité de l'en-tête avant le parsing dans G2GCOR05"
  }
]
````

## Actuator :
**actuator** : http://localhost:8080/actuator
**health** : http://localhost:8080/actuator/health
**info** : http://localhost:8080/actuator/info
**metrics** : http://localhost:8080/actuator/metrics
