import { useState, useEffect } from 'react'
import './NewsletterPreview.css'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export default function NewsletterPreview() {
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  
useEffect(() => {
  const fetchNewsletters = () => {
    console.log("📡 Fetching newsletters...")
    fetch(`${BASE_URL}/api/v1/public/newsletters`)
      .then((res) => {
        if (!res.ok) throw new Error('Failed to fetch')
        return res.json()
      })
      .then((data) => {
        setPosts(
          data.map((n) => ({
            id: n.id,
            title: n.title,
            date: formatDate(n.date),
            excerpt: n.excerpt,
          }))
        )
        setLoading(false)
      })
      .catch((err) => {
        console.error("❌ Error en fetch:", err)
        setError(true)
        setPosts([])
        setLoading(false)
      })
  }

  // primera llamada al montar
  fetchNewsletters()

  // refrescar cada 60 segundos
  const intervalId = setInterval(fetchNewsletters, 60000)

  // limpiar al desmontar
  return () => clearInterval(intervalId)
}, [])


  function formatDate(dateStr) {
    if (!dateStr) return ''
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
  }

  return (
    <section id="newsletter" className="lp-newsletter">
      <div className="lp-newsletter__container">
        <div className="lp-newsletter__header">
          <span className="lp-newsletter__badge">Newsletter</span>
          <h2 className="lp-newsletter__title">What TalentCircle generates</h2>
          <p className="lp-newsletter__subtitle">
            Real newsletter drafts automatically created from community conversations.
            Each edition is AI-generated and ready for editorial review.
          </p>
        </div>

        <div className="lp-newsletter__feed">
          {loading ? (
            <div className="lp-newsletter__loading">Loading latest newsletters...</div>
          ) : (
            posts.map((post) => (
              <article key={post.id} className="lp-newsletter__post">
                <div className="lp-newsletter__post-body">
                  <div className="lp-newsletter__post-meta">
                    <span className="lp-newsletter__post-date">{post.date}</span>
                  </div>
                  <h3 className="lp-newsletter__post-title">{post.title}</h3>
                  <p className="lp-newsletter__post-excerpt">{post.excerpt}</p>
                </div>
              </article>
            ))
          )}
        </div>

        <div className="lp-newsletter__subscribe">
          <p className="lp-newsletter__subscribe-text">
            Want these newsletters delivered to your inbox?
          </p>
          <form className="lp-newsletter__form">
            <input
              type="email"
              placeholder="Enter your email"
              className="lp-newsletter__input"
              required
            />
            <button type="submit" className="lp-newsletter__btn">
              Subscribe
            </button>
          </form>
        </div>
      </div>
    </section>
  )
}
