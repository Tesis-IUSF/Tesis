import Hero from "../components/inicio/Hero";
import Nosotros from "../components/inicio/Nosotros";
import Galeria from "../components/inicio/Galeria";
import UltimasNoticias from "../components/inicio/UltimasNoticias";
import Redes from "../components/inicio/Redes";
import "./Inicio.css";

function Inicio() {
  return (
    <div className="inicio">
      <Hero />
      <Nosotros />
      <Galeria />
      <UltimasNoticias />
      <Redes />
    </div>
  );
}

export default Inicio;
