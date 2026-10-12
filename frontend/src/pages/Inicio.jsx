import Hero from "../components/inicio/Hero";
import Nosotros from "../components/inicio/Nosotros";
import Galeria from "../components/inicio/Galeria";
import BandaAsistencia from "../components/inicio/BandaAsistencia";
import UltimasNoticias from "../components/inicio/UltimasNoticias";
import Redes from "../components/inicio/Redes";
import "./Inicio.css";

function Inicio() {
  return (
    <div className="inicio">
      <Hero />
      <Nosotros />
      <Galeria />
      <BandaAsistencia />
      <UltimasNoticias />
      <Redes />
    </div>
  );
}

export default Inicio;
