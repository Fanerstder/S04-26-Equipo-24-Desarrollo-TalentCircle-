import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import './LandingPage.css'
import Header from './components/Header/Header'
import HeroSection from './components/HeroSection/HeroSection'
import FeaturesSection from './components/FeaturesSection/FeaturesSection'
import HowItWorks from './components/HowItWorks/HowItWorks'
import NewsletterPreview from './components/NewsletterPreview/NewsletterPreview'
import CTASection from './components/CTASection/CTASection'
import Footer from './components/Footer/Footer'

export default function LandingPage() {
  const { hash } = useLocation()
  useEffect(() => {
    if (hash) {
      const el = document.querySelector(hash)
      if (el) {
        setTimeout(() => el.scrollIntoView({ behavior: 'smooth' }), 100)
      }
    }
  }, [hash])
  return (
    <div className="landing-page">
      <Header />
      <HeroSection />
      <FeaturesSection />
      <HowItWorks />
      <NewsletterPreview />
      <CTASection />
      <Footer />
    </div>
  )
}
