import { Card, Typography } from "antd";

const { Title, Paragraph } = Typography;

/**
 * Espace relecteur (F2). Contenu détaillé livré par K48-8 (liste à relire) et K48-9 (rendre la relecture).
 */
export function EspaceRelecteur() {
  return (
    <Card style={{ maxWidth: 480, margin: "2rem auto" }}>
      <Title level={3}>Espace relecteur</Title>
      <Paragraph type="secondary">
        La liste des exercices à relire arrive avec K48-8 et K48-9.
      </Paragraph>
    </Card>
  );
}
