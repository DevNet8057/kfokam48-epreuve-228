import { Card, Typography } from "antd";

const { Title, Paragraph } = Typography;

/**
 * Espace étudiant (F2). Contenu détaillé livré par K48-6 (présence), K48-7 (dépôt), K48-14 (note).
 */
export function EspaceEtudiant() {
  return (
    <Card style={{ maxWidth: 480, margin: "2rem auto" }}>
      <Title level={3}>Espace étudiant</Title>
      <Paragraph type="secondary">
        Marquer sa présence et déposer son exercice arrivent avec K48-6 et K48-7.
      </Paragraph>
    </Card>
  );
}
