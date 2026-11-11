/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{svelte,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        darkbg: '#0A0E17',
        cardbg: '#111827',
        bordercol: '#1F2937',
        meshgreen: '#10B981',
        meshcyan: '#06B6D4',
      }
    },
  },
  plugins: [],
}
