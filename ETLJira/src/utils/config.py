import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

class Config:
    """
    Configuration class to load environment variables for JIRA ETL pipeline
    """
    def __init__(self):
        # JIRA connection
        self.JIRA_BASE_URL = os.getenv("JIRA_BASE_URL", "")
        self.JIRA_USERNAME = os.getenv("JIRA_USERNAME", "")
        self.JIRA_PASSWORD = os.getenv("JIRA_PASSWORD", "")
        self.JIRA_PROJECT = os.getenv("JIRA_PROJECT", "")

        # JIRA search
        self.BATCH_SIZE = os.getenv("BATCH_SIZE", 50)

        # Logging
        self.DEBUG_LEVEL = os.getenv("DEBUG_LEVEL", "INFO")
        self.LOGS_DIR = os.getenv("LOGS_DIR", "./logs")
        self.LOGGER_NAME = os.getenv("LOGGER_NAME", "jira_logger")
        
        self.ETL_OUTPUT_DIR = os.getenv("ETL_OUTPUT_DIR", "data")


    def __repr__(self):
        return (
            f"Config(JIRA_BASE_URL={self.JIRA_BASE_URL}, "
            f"JIRA_USERNAME={self.JIRA_USERNAME}, "
            f"JIRA_PROJECT={self.JIRA_PROJECT}, "
            f"BATCH_SIZE={self.BATCH_SIZE}, "
            f"DEBUG_LEVEL={self.DEBUG_LEVEL}, "
            f"LOGS_DIR={self.LOGS_DIR}, "
            f"LOGGER_NAME={self.LOGGER_NAME})"
            f"ETL_OUTPUT_DIR={self.ETL_OUTPUT_DIR})"
        )
