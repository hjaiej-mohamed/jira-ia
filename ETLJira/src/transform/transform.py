
from utils.config import Config
import logging
import unicodedata
import re


class Transform:
    def __init__(self, config:Config):
        self.config=config
        self.logger = logging.getLogger(config.LOGGER_NAME) 

    def transform_issue(self,issue, fields_metadata):
        """
        Renomme automatiquement tous les champs personnalisés d'une issue Jira
        en utilisant les métadonnées des champs pour générer des noms normalisés.
        
        Args:
            issue (dict): Les données d'une issue Jira sous forme de dictionnaire
            fields_metadata (list): Liste des métadonnées des champs
                                (ex: [{"id": "customfield_15880", "name": "Statut Option"}, ...])
        
        Returns:
            dict: Dictionnaire d'issue modifié avec tous les champs personnalisés renommés
        """
        if not isinstance(issue, dict):
            self.logger.error("L'issue doit être un dictionnaire")
            raise ValueError("L'issue doit être un dictionnaire")
        
        if 'fields' not in issue:
            self.logger.error("L'issue doit contenir la clé 'fields'")
            raise ValueError("L'issue doit contenir la clé 'fields'")
        
        if not isinstance(fields_metadata, list):
            self.logger.error("fields_metadata doit être une liste")
            raise ValueError("fields_metadata doit être une liste")
        
        # Étape 1: Récupérer les noms des champs personnalisés
        custom_fields_names = self.get_custom_fields_names(issue)
        
        # Étape 2: Pour chaque champ personnalisé, générer et appliquer le renommage
        for old_name in custom_fields_names:
            # Générer le nouveau nom normalisé à partir des métadonnées
            new_name = self.get_normalized_field_name(fields_metadata, old_name)
            
            # Si un nom valide est généré et différent de l'original
            if new_name and new_name != old_name:
                # Appliquer le renommage (la fonction gère déjà les vérifications)
                self.rename_customfield(issue, old_name, new_name)
        
        return issue


    def get_correspond_name_by_field_id(self,fields_metadata, field_name):
        """
        Retourne le nom du champ selon son ID dans une liste de champs Jira.
        
        Args:
            fields_list (list): Liste de dictionnaires représentant les champs
            field_id (str): L'ID du champ à rechercher (ex: "issuetype", "customfield_15880")
        
        Returns:
            str or None: Le nom du champ correspondant, ou None si non trouvé
        """
        if not isinstance(fields_metadata, list):
            self.logger.error("fields_list doit être une liste")
            raise ValueError("fields_list doit être une liste")
        
        if not isinstance(field_name, str):
            self.logger.error("field_id doit être une chaîne de caractères")
            raise ValueError("field_id doit être une chaîne de caractères")
        
        for field in fields_metadata:
            if isinstance(field, dict) and field.get("id") == field_name:
                self.logger.debug(f"Champ trouvé: {field_name} -> {field.get('name')}")
                return field.get("name")
        
        self.logger.warning(f"Champ avec ID '{field_name}' non trouvé")
        return None


    def normalize_field_name(self,field_name):
        """
        Normalise un nom de champ : concatène, met en minuscules et supprime les accents.
        
        Args:
            field_name (str): Nom du champ à normaliser
            
        Returns:
            str: Nom normalisé sans accents, en minuscules
        """
        if not field_name or not isinstance(field_name, str):
            self.logger.warning(f"Nom de champ invalide: {field_name}")
            return ""
        
        # Étape 1: Supprimer les accents et caractères spéciaux
        normalized = unicodedata.normalize('NFKD', field_name)
        without_accents = ""
        for char in normalized:
            if unicodedata.combining(char):
                continue
            without_accents += char
        
        # Étape 2: Remplacer les espaces par des tirets et nettoyer
        cleaned = re.sub(r'\s+', '-', without_accents)
        cleaned = re.sub(r'[^a-zA-Z0-9\-]', '-', cleaned)
        cleaned = re.sub(r'-+', '-', cleaned).strip('-')
        
        # Mettre en minuscules
        result = cleaned.lower()
        
        self.logger.debug(f"Nom normalisé: '{field_name}' -> '{result}'")
        return result


    def get_normalized_field_name(self,fields_list, field_id):
        """
        Combine les deux fonctions : récupère le nom et le normalise.
        
        Args:
            fields_list (list): Liste des champs
            field_id (str): ID du champ
            
        Returns:
            str: Nom normalisé du champ
        """
        field_name = self.get_correspond_name_by_field_id(fields_list, field_id)
        if field_name:
            return self.normalize_field_name(field_name)
        self.logger.debug(f"Aucun nom normalisé pour field_id: {field_id}")
        return ""


    def rename_customfield(self,issue, old_name, new_name):
        """
        Renomme un champ personnalisé dans un dictionnaire d'issue Jira.
        
        Args:
            issue (dict): Les données d'une issue Jira sous forme de dictionnaire
            old_name (str): Le nom actuel du champ personnalisé 
            new_name (str): Le nouveau nom pour le champ personnalisé
        
        Returns:
            dict: Dictionnaire d'issue modifié avec le champ renommé
        """
        if not isinstance(issue, dict):
            self.logger.error("L'issue doit être un dictionnaire")
            raise ValueError("L'issue doit être un dictionnaire")
        
        if 'fields' not in issue:
            self.logger.error("L'issue doit contenir la clé 'fields'")
            raise ValueError("L'issue doit contenir la clé 'fields'")
        
        fields = issue['fields']
        
        if old_name not in fields:
            self.logger.warning(f"Champ personnalisé '{old_name}' non trouvé dans l'issue")
            return issue
        
        if new_name in fields:
            self.logger.warning(f"Nouveau nom de champ '{new_name}' existe déjà. Écrasement...")
        
        fields[new_name] = fields.pop(old_name)
        self.logger.info(f"Le champ personnalisé '{old_name}' a été renommé en '{new_name}'")
        
        return issue


    def get_custom_fields_names(self,issue):
        """
        Extrait tous les champs personnalisés (commençant par 'customfield_') d'une issue Jira.
        
        Args:
            issue (dict): Données de l'issue Jira
            
        Returns:
            list: Liste des noms de champs personnalisés
        """
        if not isinstance(issue, dict) or 'fields' not in issue:
            self.logger.error("Structure d'issue Jira invalide")
            raise ValueError("Structure d'issue Jira invalide")
        
        fields = issue['fields']
        custom_field_names = [
            field_name for field_name in fields.keys()
            if field_name.startswith('customfield_')
        ]
        
        self.logger.info(f"{len(custom_field_names)} champs personnalisés trouvés: {custom_field_names}")
        return custom_field_names