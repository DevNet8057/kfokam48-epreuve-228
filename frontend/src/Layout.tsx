import { Button, Layout as AntLayout, Menu, Space, Typography } from "antd";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useIdentite } from "./identite/IdentiteContext";

const { Header, Content } = AntLayout;
const { Text } = Typography;

const ESPACES = [
  { key: "/formateur", label: "Formateur" },
  { key: "/etudiant", label: "Étudiant" },
  { key: "/relecteur", label: "Relecteur" },
];

/**
 * F2 : les trois espaces (formateur, étudiant, relecteur) sont accessibles depuis une navigation commune.
 * K48-25 : l'identité courante (nom) et le bouton « Changer d'identité » sont affichés dans l'en-tête.
 */
export function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { identite, changerIdentite } = useIdentite();

  function seDeconnecter() {
    changerIdentite();
    navigate("/");
  }

  return (
    <AntLayout style={{ minHeight: "100vh" }}>
      <Header style={{ display: "flex", alignItems: "center", gap: 16 }}>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[location.pathname]}
          items={ESPACES.map((espace) => ({
            key: espace.key,
            label: <Link to={espace.key}>{espace.label}</Link>,
          }))}
          style={{ flex: 1, minWidth: 0 }}
        />
        {identite && (
          <Space>
            {identite.nom && <Text style={{ color: "white" }}>{identite.nom}</Text>}
            <Button size="small" onClick={seDeconnecter}>
              Changer d'identité
            </Button>
          </Space>
        )}
      </Header>
      <Content style={{ padding: "16px" }}>
        <Outlet />
      </Content>
    </AntLayout>
  );
}
