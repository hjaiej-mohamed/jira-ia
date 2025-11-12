from utils.config import Config
from utils.jira_client import JiraClient
from utils.file_manager import FileManager
import logging


class Load:
    def __init__(self,config:Config,jira_client:JiraClient):
        self.config=config
        self.file_manager = FileManager(config=config)
        self.jira_client= jira_client
        self.logger = logging.getLogger(self.config.LOGGER_NAME)

    def save_issue(self,issue):
        issue_key = issue.get('key')
        issue_dir = self.file_manager.create_issue_folder(self.config.ETL_OUTPUT_DIR, issue_key)
        self.file_manager.save_json(issue, issue_dir, issue_key)
        attachments = issue.get("fields", {}).get("attachment", [])

        if attachments:
            self.jira_client.download_attachments(issue_key, attachments, issue_dir)
        else:
            self.logger.info(f"[{issue_key}] Pas d'attachments.")


    