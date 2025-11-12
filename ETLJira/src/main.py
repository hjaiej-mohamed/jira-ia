from utils.config import Config
from utils.jira_client import JiraClient
from utils.logger import setup_logging
from pipeline.pipeline import Pipeline


config = Config()
setup_logging(config.LOGS_DIR,config.DEBUG_LEVEL,config.LOGGER_NAME)

jira_client = JiraClient(config=config)
pipeline = Pipeline(config=config,jira_client=jira_client)
pipeline.run()


