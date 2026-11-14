export default function Stamp({ className = "" }) {
  return (
    <div
      className={`pointer-events-none flex h-24 w-24 -rotate-12 items-center justify-center rounded-full border-2 border-dashed border-brass-500/70 text-brass-600 ${className}`}
    >
      <svg viewBox="0 0 100 100" className="h-full w-full">
        <path id="stamp-circle" fill="none" d="M 50,50 m -34,0 a 34,34 0 1,1 68,0 a 34,34 0 1,1 -68,0" />
        <text className="fill-current" style={{ fontFamily: "var(--font-display)", fontSize: "10.5px", letterSpacing: "2.5px" }}>
          <textPath href="#stamp-circle" startOffset="2%">
            FAIT MAIN · CASABLANCA · FAIT MAIN ·
          </textPath>
        </text>
        <text
          x="50"
          y="55"
          textAnchor="middle"
          className="fill-current"
          style={{ fontFamily: "var(--font-script)", fontSize: "22px" }}
        >
          100%
        </text>
      </svg>
    </div>
  );
}
