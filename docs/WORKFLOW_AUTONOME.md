# Workflow autonome des agents

Les agents lisent le ticket Jira, son issue GitHub jumelle et la documentation citée avant de créer une branche dédiée. Ils font progresser Jira de `À faire` à `Backend en cours`, `Frontend en cours`, `En revue`, `Prêt à tester`, puis `Terminé`.

Le reviewer relit le diff, vérifie le contrat, exécute `scripts/verify.sh` et contrôle les critères d'acceptation. Une PR conforme est fusionnée par merge commit. La recette automatisée est ensuite lancée sur `main`.

Un échec crée un ticket Bug Jira/GitHub lié, conserve les preuves dans la PR et passe par une branche `fix/`. Le hook `.githooks/pre-push` lance `scripts/verify.sh` avant chaque push. Le workflow GitHub reste disponible au lancement manuel quand les runners hébergés sont disponibles.
