import { OuvertureSession } from "./OuvertureSession";
import { TableauFormateur } from "./TableauFormateur";

/**
 * Espace formateur (F2) : ouvrir une session (EF1) et consulter le tableau de la promotion (EF10).
 */
export function EspaceFormateur() {
  return (
    <>
      <OuvertureSession />
      <TableauFormateur />
    </>
  );
}
