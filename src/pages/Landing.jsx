import Nav from "../components/landing/Nav";
import Hero from "../components/landing/Hero";
import WhoItsFor from "../components/landing/WhoItsFor";
import StatementBand from "../components/landing/StatementBand";
import ProductShowcase from "../components/landing/ProductShowcase";
import HowItWorks from "../components/landing/HowItWorks";
import Pricing from "../components/landing/Pricing";
import FinalCTA from "../components/landing/FinalCTA";
import Footer from "../components/landing/Footer";
import FloatingWhatsApp from "../components/ui/FloatingWhatsApp";

export default function Landing() {
  return (
    <div className="min-h-screen bg-cream">
      <div aria-hidden="true" className="grain-overlay" />
      <Nav />
      <main>
        <Hero />
        <WhoItsFor />
        <StatementBand />
        <ProductShowcase />
        <HowItWorks />
        <Pricing />
        <FinalCTA />
      </main>
      <Footer />
      <FloatingWhatsApp />
    </div>
  );
}
