import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50:  '#ecf3fc',
          100: '#dde3fa',
          200: '#c5caee',
          300: '#9ea3dc',
          500: '#473deb',
          600: '#473deb',
          700: '#2b21c4',
          800: '#2b0a5b',
          900: '#1a0637',
        },
      },
    },
  },
  plugins: [],
};

export default config;
