from utils.config import Config
from utils.jira_client import JiraClient
import logging
import math

class Extract:
    def __init__(self,config:Config,jira_client:JiraClient):
        self.config = config
        self.jira_client = jira_client
        self.logger = logging.getLogger(config.LOGGER_NAME)
        self.BATCH_SIZE = int(config.BATCH_SIZE)

    def extract_batch_issues(self,start:int =0,batch_size:int=50):
        batch_issues = self.jira_client.getBatchIssues(start,batch_size)
        self.logger.info(f"[BATCH 1]: nombre d'issues {len(batch_issues['issues'])}...")
        return batch_issues['issues']

    def getNbTotalIssuer(self):
        total = self.jira_client.countTotalIussues(self.config.JIRA_PROJECT)
        self.logger.info(f"Nombre total d'issues trouvés: {total}.")
        return total

