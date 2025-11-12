from utils.config import Config
from utils.jira_client import JiraClient
from extract.extract import Extract
from transform.transform import Transform
from load.load import Load
import math
import threading
import logging
from concurrent.futures import ThreadPoolExecutor


class Pipeline:
    def __init__(self,config: Config,jira_client=JiraClient):
        self.config= config
        self.jira_client = jira_client
        self.extracter = Extract(config=config,jira_client=jira_client)
        self.transformer = Transform(config=config)
        self.loader = Load(config=config,jira_client=jira_client)
        self.logger = logging.getLogger(self.config.LOGGER_NAME)

    def run(self):
        nbIssues = self.jira_client.countTotalIussues(self.config.JIRA_PROJECT)
        iterations = math.ceil(nbIssues / int(self.config.BATCH_SIZE))

        with ThreadPoolExecutor(max_workers=5) as executor:
            futures = []
            for i in range(iterations):
                start_index = i * int(self.config.BATCH_SIZE)
                # submit each batch to the thread pool
                futures.append(executor.submit(self.run_batch_with_log, start_index))

            # Wait for all batches to finish
            for future in futures:
                future.result()

    def run_batch_with_log(self, start_index):
        thread_name = threading.current_thread().name
        self.logger.info(f"[Thread {thread_name}] Processing batch starting at index {start_index}")
        self.run_batch(start_index)
    def run_batch(self,start):
        jira_fields_metadata= self.get_metadata()
        batch_issues = self.extracter.extract_batch_issues(start, self.config.BATCH_SIZE)
        for issue in batch_issues:
            transfomred_issue= self.transformer.transform_issue(issue,jira_fields_metadata)
            self.loader.save_issue(transfomred_issue)
    def get_metadata(self):
        return self.jira_client.get_jira_fields_metadata()
    
