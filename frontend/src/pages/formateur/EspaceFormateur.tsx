import { GestionSessions } from "./GestionSessions";
import { OuvertureSession } from "./OuvertureSession";
import { TableauFormateur } from "./TableauFormateur";

/**
 * Espace formateur (F2) : ouvrir une session (EF1), gérer les sessions (EF5) et consulter le tableau (EF10).
 */
export function EspaceFormateur() {
  return (
    <>
      <OuvertureSession />
      <GestionSessions />
      <TableauFormateur />
    </>
  );
}
