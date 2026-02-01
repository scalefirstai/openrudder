# OpenRudder Website

Modern documentation website for OpenRudder with K8sGPT-inspired design.

## Features

- **K8sGPT Color Theme**: Blue/purple gradient aesthetic with dark mode
- **Responsive Design**: Mobile-friendly layout
- **Interactive Documentation**: Smooth scrolling, code copying, active section highlighting
- **Modern UI**: Animations, gradients, and glassmorphism effects

## Structure

```
website/
├── index.html          # Homepage with features and getting started
├── docs.html           # Comprehensive documentation
├── styles.css          # Main stylesheet (K8sGPT theme)
├── docs-styles.css     # Documentation-specific styles
├── script.js           # Homepage interactivity
├── docs-script.js      # Documentation page scripts
└── README.md           # This file
```

## Color Theme

Based on K8sGPT's design:

- **Primary Blue**: `#3b82f6`
- **Primary Purple**: `#8b5cf6`
- **Primary Indigo**: `#6366f1`
- **Background Dark**: `#0f172a`
- **Background Darker**: `#020617`
- **Card Background**: `#1e293b`

## Running Locally

### Option 1: Simple HTTP Server (Python)

```bash
cd website
python3 -m http.server 8000
```

Visit: http://localhost:8000

### Option 2: Node.js HTTP Server

```bash
cd website
npx http-server -p 8000
```

### Option 3: VS Code Live Server

1. Install "Live Server" extension
2. Right-click `index.html`
3. Select "Open with Live Server"

## Deployment Options

### GitHub Pages

1. Push to GitHub repository
2. Go to Settings → Pages
3. Select branch and `/website` folder
4. Save

### Netlify

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Deploy
cd website
netlify deploy --prod
```

### Vercel

```bash
# Install Vercel CLI
npm install -g vercel

# Deploy
cd website
vercel --prod
```

### Static Hosting (S3, Azure, etc.)

Simply upload all files in the `website/` directory to your static hosting service.

## Customization

### Update Colors

Edit `styles.css` `:root` variables:

```css
:root {
    --primary-blue: #3b82f6;
    --primary-purple: #8b5cf6;
    /* ... */
}
```

### Add Documentation Sections

Edit `docs.html` and add new sections:

```html
<section id="new-section" class="doc-section">
    <h2>New Section</h2>
    <p>Content here...</p>
</section>
```

Update sidebar in `docs.html`:

```html
<li><a href="#new-section">New Section</a></li>
```

### Modify Features

Edit the features grid in `index.html`:

```html
<div class="feature-card">
    <div class="feature-icon"><!-- SVG icon --></div>
    <h3>Feature Title</h3>
    <p>Feature description</p>
</div>
```

## Browser Support

- Chrome/Edge: ✅ Full support
- Firefox: ✅ Full support
- Safari: ✅ Full support
- Mobile browsers: ✅ Responsive design

## Performance

- Minimal dependencies (no frameworks)
- Optimized CSS with CSS variables
- Lazy loading for animations
- Fast load times (<1s)

## Accessibility

- Semantic HTML5
- ARIA labels where needed
- Keyboard navigation support
- High contrast ratios
- Focus indicators

## License

Same as OpenRudder project (Apache 2.0)
