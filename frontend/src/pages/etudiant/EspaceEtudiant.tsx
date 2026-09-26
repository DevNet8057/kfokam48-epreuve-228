import { MarquerPresence } from "./MarquerPresence";
import { MesExercices } from "./MesExercices";

/**
 * Espace étudiant (F2) : marquer sa présence et déposer un exercice (MarquerPresence),
 * consulter la note reçue (EF11, K48-14).
 */
export function EspaceEtudiant() {
  return (
    <>
      <MarquerPresence />
      <MesExercices />
    </>
  );
}
