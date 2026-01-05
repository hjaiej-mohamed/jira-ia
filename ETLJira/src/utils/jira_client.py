
import requests
from requests.auth import HTTPBasicAuth
from .config import Config
import os
import logging

class JiraClient:
    def __init__(self,config:Config):
        self.jira_base_url=config.JIRA_BASE_URL
        self.jira_username=config.JIRA_USERNAME
        self.jira_password=config.JIRA_PASSWORD
        self.jira_project=config.JIRA_PROJECT
            # Create a session for connection reuse
        self.session = requests.Session()
        self.session.auth = HTTPBasicAuth(self.jira_username, self.jira_password)
        self.session.headers.update({
            "Accept": "application/json",
            "Content-Type": "application/json"
        })
        self.logger = logging.getLogger(config.LOGGER_NAME)
    def countTotalIussues(self,project:str)->int:
        url = f"{self.jira_base_url}/rest/api/2/search"
        params={
            "jql": f"project={self.jira_project}",
            "startAt":0,
            "maxResults":0,
            "fields":"none"
        }
        response = self.session.get(url,params=params)
        self._check_response(response)
        data=response.json()

        return data["total"]
    
    def getBatchIssues(self,start,maxResults):
        url = f"{self.jira_base_url}/rest/api/2/search"
        params = {
            "jql": f"project={self.jira_project}",
            "startAt": start,
            "maxResults": maxResults,
            "fields": "*all",          # fetch all fields
            "expand": "renderedFields" # always include rendered fields
        }
        response = self.session.get(url,params=params)
        self._check_response(response)
        data=response.json()

        return data
    
    def download_attachments(self, issue_key, attachments, issue_dir):
        for attachment in attachments:
            file_url = attachment["content"]
            file_name = attachment["filename"]
            try:
                response = self.session.get(file_url)
                if response.status_code == 200:
                    file_path = os.path.join(issue_dir, file_name)
                    with open(file_path, "wb") as f:
                        f.write(response.content)
                    self.logger.info(f"[{issue_key}] Attachment sauvegardé: {file_name}")
                else:
                    self.logger.warning(f"[{issue_key}] Probleme de chargement {file_name}: HTTP {response.status_code}")
            except Exception as e:
                self.logger.error(f"[{issue_key}] Probleme de chargement {file_name}: {e}")

    
    def _check_response(self, response):
        if not response.ok: 
            raise Exception(
                f"JIRA API Error {response.status_code}: {response.text}"
            )


    def get_jira_fields_metadata(self):
        url = f"{self.jira_base_url}/rest/api/2/field"
        response = self.session.get(url)
        self._check_response(response)
        return response.json()