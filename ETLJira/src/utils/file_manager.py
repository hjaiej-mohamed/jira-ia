# utils/file_manager.py
import os
import json
import logging
from utils.config import Config
import requests


class FileManager:
    def __init__(self, config:Config):
        self.config=config
        self.logger = logging.getLogger(config.LOGGER_NAME)
    def create_issue_folder(self,base_dir, issue_key):
        issue_dir = os.path.join(base_dir, issue_key)
        os.makedirs(issue_dir, exist_ok=True)
        self.logger.debug(f"Dossier créé/vérifié : {issue_dir}")
        return issue_dir

    def save_json(self,data, folder, issue_key):
        file_path = os.path.join(folder, f"{issue_key}.json")
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=4, ensure_ascii=False)
        self.logger.info(f"[{issue_key}] JSON sauvegardé : {file_path}")
    
        
    # def download_attachments(self,issue_key, attachments, issue_dir, auth):
    #     for attachment in attachments:
    #         file_url = attachment["content"]
    #         file_name = attachment["filename"]
    #         response = requests.get(file_url, auth=auth)
    #         if response.status_code == 200:
    #             with open(f"{issue_dir}/{file_name}", "wb") as f:
    #                 f.write(response.content)
