# utils/logger.py
import logging
import os
from logging.handlers import RotatingFileHandler
import sys

def setup_logging(log_dir: str, level: str = "INFO", logger_name: str = "jira_export"):
    os.makedirs(log_dir, exist_ok=True)
    log_file = os.path.join(log_dir, "export.log")

    logger = logging.getLogger(logger_name)
    logger.setLevel(level.upper())
    logger.handlers.clear()  

    fmt = logging.Formatter(
        fmt="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    # Console
    sh = logging.StreamHandler(sys.stdout)
    sh.setLevel(level.upper())
    sh.setFormatter(fmt)
    logger.addHandler(sh)

    # Fichier avec rotation (50 Mo, 5 backups)
    fh = RotatingFileHandler(log_file, maxBytes=50 * 1024 * 1024, backupCount=5, encoding="utf-8")
    fh.setLevel(level.upper())
    fh.setFormatter(fmt)
    logger.addHandler(fh)

    # Réduire le bruit des libs
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    logging.getLogger("requests").setLevel(logging.WARNING)

    logger.debug("Logging initialisé (console + fichier).")
    return logger