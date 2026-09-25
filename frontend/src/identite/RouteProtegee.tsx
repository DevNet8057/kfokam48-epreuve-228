import { Navigate } from "react-router-dom";
import { Profil, useIdentite } from "./IdentiteContext";

interface Props {
  profilRequis: Profil;
  children: React.ReactNode;
}

/** K48-25 : sans identité (ou mauvais profil), retour au choix d'identité. */
export function RouteProtegee({ profilRequis, children }: Props) {
  const { identite } = useIdentite();

  if (identite?.profil !== profilRequis) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
