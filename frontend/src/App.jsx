import { useState, useCallback } from 'react'

const API_BASE = import.meta.env.VITE_API_URL || '/api'

function formatTime(iso) {
  try {
    return new Date(iso).toLocaleString(undefined, {
      month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
    })
  } catch {
    return iso
  }
}

function formatDuration(mins) {
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h ? `${h}h ${m}m` : `${m}m`
}

export default function App() {
  const [filters, setFilters] = useState({
    origin: '',
    destination: '',
    max_price: '',
    carrier: '',
    max_stops: '',
    limit: 20,
  })
  const [results, setResults] = useState([])
  const [cursor, setCursor] = useState(null)
  const [nextCursor, setNextCursor] = useState(null)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(false)
  const [ingesting, setIngesting] = useState(false)
  const [error, setError] = useState(null)
  const [ingestInfo, setIngestInfo] = useState(null)
  const [history, setHistory] = useState([]) // stack of cursors for "prev"
  const [hasSearched, setHasSearched] = useState(false)

  const buildQuery = useCallback((cursorVal) => {
    const params = new URLSearchParams()
    if (filters.origin) params.set('origin', filters.origin.toUpperCase())
    if (filters.destination) params.set('destination', filters.destination.toUpperCase())
    if (filters.max_price) params.set('maxPrice', filters.max_price)
    if (filters.carrier) params.set('carrier', filters.carrier.toUpperCase())
    if (filters.max_stops !== '') params.set('maxStops', filters.max_stops)
    params.set('limit', filters.limit)
    if (cursorVal) params.set('cursor', cursorVal)
    return params.toString()
  }, [filters])

  const search = async (cursorVal = null, pushHistory = false) => {
    setHasSearched(true)
    setLoading(true)
    setError(null)
    try {
      const qs = buildQuery(cursorVal)
      const res = await fetch(`${API_BASE}/search?${qs}`)
      if (!res.ok) throw new Error(`Search failed: ${res.status}`)
      const data = await res.json()
      setResults(data.items || [])
      setNextCursor(data.next_cursor ?? data.nextCursor ?? null)
      setHasMore(Boolean(data.has_more ?? data.hasMore))
      setCursor(cursorVal)
      if (pushHistory && cursorVal) {
        setHistory((h) => [...h, cursorVal])
      }
    } catch (e) {
      setError(e.message)
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = () => {
    setHistory([])
    search(null)
  }

  const handleNext = () => {
    if (nextCursor) search(nextCursor, true)
  }

  const handlePrev = () => {
    if (history.length === 0) {
      search(null)
      return
    }
    const prev = [...history]
    prev.pop()
    setHistory(prev)
    search(prev.length ? prev[prev.length - 1] : null)
  }

  const handleIngest = async () => {
    setIngesting(true)
    setError(null)
    setIngestInfo(null)
    try {
      const params = new URLSearchParams()
      if (filters.origin) params.set('origin', filters.origin.toUpperCase())
      if (filters.destination) params.set('destination', filters.destination.toUpperCase())
      const res = await fetch(`${API_BASE}/ingest?${params}`, { method: 'POST' })
      if (!res.ok) throw new Error(`Ingest failed: ${res.status}`)
      const data = await res.json()
      setIngestInfo(data)
      // auto-refresh search after ingest
      handleSearch()
    } catch (e) {
      setError(e.message)
    } finally {
      setIngesting(false)
    }
  }

  const onFilter = (key, value) => {
    setFilters((f) => ({ ...f, [key]: value }))
  }

  return (
    <div className="app">
      <header>
        <div>
          <h1>✈ Flight Aggregation Dashboard</h1>
          <div className="badge">Multi-GDS Provider · Keyset Pagination</div>
        </div>
      </header>

      <div className="card">
        <div className="filters">
          <label>
            Origin
            <input
              placeholder="JFK"
              maxLength={3}
              value={filters.origin}
              onChange={(e) => onFilter('origin', e.target.value)}
            />
          </label>
          <label>
            Destination
            <input
              placeholder="LAX"
              maxLength={3}
              value={filters.destination}
              onChange={(e) => onFilter('destination', e.target.value)}
            />
          </label>
          <label>
            Max Price ($)
            <input
              type="number"
              placeholder="500"
              min="0"
              value={filters.max_price}
              onChange={(e) => onFilter('max_price', e.target.value)}
            />
          </label>
          <label>
            Carrier
            <input
              placeholder="AA"
              maxLength={3}
              value={filters.carrier}
              onChange={(e) => onFilter('carrier', e.target.value)}
            />
          </label>
          <label>
            Max Stops
            <select value={filters.max_stops} onChange={(e) => onFilter('max_stops', e.target.value)}>
              <option value="">Any</option>
              <option value="0">Non-stop</option>
              <option value="1">≤ 1 stop</option>
              <option value="2">≤ 2 stops</option>
            </select>
          </label>
          <label>
            Page size
            <select value={filters.limit} onChange={(e) => onFilter('limit', Number(e.target.value))}>
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </label>
          <div className="actions">
            <button onClick={handleSearch} disabled={loading}>
              Search
            </button>
            <button className="secondary" onClick={handleIngest} disabled={ingesting}>
              {ingesting ? 'Ingesting…' : 'Ingest from GDS'}
            </button>
          </div>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {ingestInfo && (
        <div className="status-bar">
          <span>Ingested: <strong>{ingestInfo.ingested}</strong></span>
          <span>Raw collected: <strong>{ingestInfo.rawCollected}</strong></span>
          <span>After dedup: <strong>{ingestInfo.afterDedupAndPricing}</strong></span>
          <span>Providers OK: <strong>{ingestInfo.providersOk?.join(', ') || '—'}</strong></span>
          {ingestInfo.providersFailed?.length > 0 && (
            <span style={{ color: 'var(--warning)' }}>
              Failed: {ingestInfo.providersFailed.map((f) => f.provider).join(', ')}
            </span>
          )}
        </div>
      )}

      {loading ? (
        <div className="loading"><div className="spinner" /> Loading itineraries…</div>
      ) : results.length === 0 ? (
            <div className="empty">
              <h3>{hasSearched ? 'No matching itineraries' : 'No itineraries yet'}</h3>
              <p>
                {hasSearched
                  ? 'Try relaxing filters (price, stops, carrier) or ingest fresh data.'
                  : 'Click “Ingest from GDS” to pull live data from Mock-Alpha, Mock-Beta & Mock-Gamma, then search.'}
              </p>
            </div>
      ) : (
        <>
          <div className="status-bar">
            <span>Showing <strong>{results.length}</strong> results</span>
            {hasMore && <span>More available →</span>}
          </div>
          <div className="results">
            {results.map((itinerary) => {
              const finalPrice = itinerary.final_price ?? itinerary.finalPrice
              const basePrice = itinerary.base_price ?? itinerary.basePrice
              const flightNumbers = itinerary.flight_numbers ?? itinerary.flightNumbers ?? []
              const segments = itinerary.segments ?? []

              return (
                 <div className="itin" key={itinerary.id}>
                    <div className="itin-main">
                      <div className="route">
                        {itinerary.origin} <span className="arrow">→</span> {itinerary.destination}
                        <span className="pill">{itinerary.stops === 0 ? 'Non-stop' : `${itinerary.stops} stop${itinerary.stops > 1 ? 's' : ''}`}</span>
                      </div>
                      <div className="meta">
                        <span>{formatTime(itinerary.departure)} – {formatTime(itinerary.arrival)}</span>
                        <span>{formatDuration(itinerary.durationMinutes)}</span>
                        <span className="pill">{Array.isArray(itinerary.flightNumbers) ? itinerary.flightNumbers.join(' · ') : 'N/A'}</span>
                        <span className="pill">{itinerary.cabinClass}</span>
                        <span className="pill">{itinerary.seatsAvailable} seats</span>
                      </div>
                      <div className="segments">
                        {(Array.isArray(itinerary.segments) ? itinerary.segments : []).map((s, i) => (
                          <div className="seg-row" key={i}>
                            <strong>{s.airline}{s.flightNumber}</strong>
                            <span>{s.origin}→{s.destination}</span>
                            <span>{formatTime(s.departure)}</span>
                            <span>{formatDuration(s.durationMinutes)}</span>
                            {s.aircraft && <span>{s.aircraft}</span>}
                          </div>
                        ))}
                      </div>
                  </div>
                  <div className="price-block">
                    <div className="price">
                      {finalPrice != null ? `$${Number(finalPrice).toFixed(2)}` : 'N/A'}
                    </div>
                    <div className="base-price">
                      {basePrice != null ? `$${Number(basePrice).toFixed(2)}` : 'N/A'}
                    </div>
                    <div className="provider">{itinerary.provider}</div>
                  </div>
                </div>
              )
            })}
          </div>
          <div className="pagination">
            <button className="secondary" onClick={handlePrev} disabled={!cursor && history.length === 0}>
              ← Previous
            </button>
            <button onClick={handleNext} disabled={!hasMore}>
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  )
}
