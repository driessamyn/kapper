<template>
  <div class="changelog-container">
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>Loading changelog from GitHub...</p>
    </div>

    <div v-else-if="error" class="error">
      <h3>⚠️ Unable to load changelog</h3>
      <p>
        Please visit the 
        <a href="https://github.com/driessamyn/kapper/releases" target="_blank" rel="noopener">
          GitHub Releases page
        </a> 
        for the latest changelog information.
      </p>
      <details v-if="error">
        <summary>Error details</summary>
        <pre>{{ error }}</pre>
      </details>
    </div>

    <div v-else class="changelog">
      <div v-for="release in releases" :key="release.tag" class="release">
        <div class="release-header">
          <h2>
            <a :href="release.url" target="_blank" rel="noopener">
              {{ release.name }}
            </a>
            <span v-if="release.isPrerelease" class="prerelease-badge">
              Pre-release
            </span>
          </h2>
          <p class="release-meta">
            <strong>{{ release.tag }}</strong> • 
            Released {{ formatDate(release.date) }}
          </p>
        </div>
        
        <div class="release-body" v-html="parseMarkdown(release.body)"></div>
      </div>
      
      <div class="changelog-footer">
        <p>
          <strong>Note:</strong> This changelog is automatically generated from 
          <a href="https://github.com/driessamyn/kapper/releases" target="_blank" rel="noopener">
            GitHub Releases
          </a>.
          Last updated: {{ lastUpdated }}.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const releases = ref([])
const loading = ref(true)
const error = ref(null)
const lastUpdated = ref('')

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long', 
    day: 'numeric'
  })
}

const parseMarkdown = (body) => {
  if (!body) return '<p><em>No release notes available.</em></p>';

  // Basic HTML entity escaping to prevent XSS
  const escapeHtml = (unsafe) => {
    return unsafe
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  };

  let html = body
    // 1. Escape all HTML entities to prevent XSS
    .split('\n').map(escapeHtml).join('\n')
    // 2. Process markdown features
    // Code blocks (must run before other replacements)
    .replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => `<pre><code>${code}</code></pre>`)
    // Bold text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // Italic text
    .replace(/(?<!\*)\*([^*\n]+)\*(?!\*)/g, '<em>$1</em>')
    // Inline code
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // Links
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');

  // 3. Process block elements line-by-line
  const lines = html.split('\n');
  let inList = false;
  html = lines.map(line => {
    let processedLine = '';
    if (line.startsWith('# ')) {
      processedLine = `<h1>${line.substring(2)}</h1>`;
    } else if (line.startsWith('## ')) {
      processedLine = `<h2>${line.substring(3)}</h2>`;
    } else if (line.startsWith('### ')) {
      processedLine = `<h3>${line.substring(4)}</h3>`;
    } else if (line.startsWith('- ') || line.startsWith('* ')) {
      if (!inList) {
        inList = true;
        processedLine = `<ul><li>${line.substring(2)}</li>`;
      } else {
        processedLine = `<li>${line.substring(2)}</li>`;
      }
    } else {
      if (inList) {
        inList = false;
        processedLine = '</ul>';
      }
      if (line.trim()) {
        // Avoid wrapping <pre> in <p>
        if (!line.trim().startsWith('<pre>')) {
          processedLine += `<p>${line}</p>`;
        } else {
          processedLine += line;
        }
      }
    }
    return processedLine;
  }).join('');

  if (inList) {
    html += '</ul>';
  }

  return html;
}

onMounted(async () => {
  try {
    const response = await fetch('https://api.github.com/repos/driessamyn/kapper/releases')
    
    if (!response.ok) {
      throw new Error(`GitHub API responded with status ${response.status}: ${response.statusText}`)
    }
    
    const data = await response.json()
    
    releases.value = data
      .filter(release => !release.draft) // Only published releases
      .slice(0, 12) // Limit to latest 12 releases
      .map(release => ({
        name: release.name || release.tag_name,
        tag: release.tag_name,
        date: release.published_at,
        body: release.body || '',
        url: release.html_url,
        isPrerelease: release.prerelease
      }))
    
    lastUpdated.value = formatDate(new Date().toISOString())
      
  } catch (err) {
    error.value = err.message
    console.error('Failed to fetch releases:', err)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.changelog-container {
  max-width: 100%;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--vp-c-bg-soft);
  border-top: 3px solid var(--vp-c-brand);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading p {
  color: var(--vp-c-text-2);
  margin: 0;
}

.error {
  padding: 2rem;
  background: var(--vp-c-danger-soft);
  border: 1px solid var(--vp-c-danger);
  border-radius: 8px;
  margin: 2rem 0;
  text-align: center;
}

.error h3 {
  margin: 0 0 1rem 0;
  color: var(--vp-c-danger);
}

.error p {
  margin: 0.5rem 0;
}

.error a {
  color: var(--vp-c-danger);
  font-weight: 600;
  text-decoration: underline;
}

.error details {
  margin-top: 1rem;
  text-align: left;
}

.error pre {
  background: var(--vp-c-bg-soft);
  padding: 1rem;
  border-radius: 4px;
  font-size: 0.875rem;
  margin-top: 0.5rem;
  overflow-x: auto;
}

.changelog {
  margin-top: 1rem;
}

.release {
  margin-bottom: 3rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid var(--vp-c-divider);
}

.release:last-of-type {
  border-bottom: none;
  margin-bottom: 1rem;
}

.release-header h2 {
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  font-size: 1.5rem;
}

.release-header h2 a {
  color: var(--vp-c-brand);
  text-decoration: none;
  transition: color 0.25s;
}

.release-header h2 a:hover {
  text-decoration: underline;
  color: var(--vp-c-brand-dark);
}

.prerelease-badge {
  background: var(--vp-c-warning-soft);
  color: var(--vp-c-warning-dark);
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  border: 1px solid var(--vp-c-warning);
  white-space: nowrap;
}

.release-meta {
  margin: 0;
  color: var(--vp-c-text-2);
  font-size: 0.9rem;
  font-weight: 500;
}

.release-body {
  margin-top: 1.25rem;
  line-height: 1.7;
}

.release-body :deep(h1) {
  font-size: 1.25rem;
  margin: 1.5rem 0 0.75rem 0;
  color: var(--vp-c-text-1);
  border-bottom: 1px solid var(--vp-c-divider);
  padding-bottom: 0.25rem;
}

.release-body :deep(h2) {
  font-size: 1.15rem;
  margin: 1.25rem 0 0.5rem 0;
  color: var(--vp-c-text-1);
}

.release-body :deep(h3) {
  font-size: 1.1rem;
  margin: 1rem 0 0.5rem 0;
  color: var(--vp-c-text-1);
  font-weight: 600;
}

.release-body :deep(p) {
  margin: 0.75rem 0;
  color: var(--vp-c-text-1);
}

.release-body :deep(p:first-child) {
  margin-top: 0;
}

.release-body :deep(ul) {
  margin: 0.75rem 0;
  padding-left: 1.5rem;
}

.release-body :deep(li) {
  margin: 0.25rem 0;
  color: var(--vp-c-text-1);
}

.release-body :deep(strong) {
  color: var(--vp-c-text-1);
  font-weight: 600;
}

.release-body :deep(em) {
  font-style: italic;
  color: var(--vp-c-text-2);
}

.release-body :deep(code) {
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-code);
  padding: 0.25rem 0.375rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-family: var(--vp-font-family-mono);
  font-weight: 500;
}

.release-body :deep(pre) {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-border);
  border-radius: 6px;
  padding: 1rem;
  margin: 1rem 0;
  overflow-x: auto;
  line-height: 1.5;
}

.release-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: var(--vp-c-text-code);
  font-size: 0.875rem;
}

.release-body :deep(a) {
  color: var(--vp-c-brand);
  text-decoration: none;
  font-weight: 500;
}

.release-body :deep(a:hover) {
  text-decoration: underline;
}

.changelog-footer {
  margin-top: 3rem;
  padding: 1.5rem;
  background: var(--vp-c-bg-soft);
  border-radius: 8px;
  text-align: center;
  border: 1px solid var(--vp-c-border);
}

.changelog-footer p {
  margin: 0;
  font-size: 0.9rem;
  color: var(--vp-c-text-2);
  line-height: 1.5;
}

.changelog-footer a {
  color: var(--vp-c-brand);
  text-decoration: none;
  font-weight: 500;
}

.changelog-footer a:hover {
  text-decoration: underline;
}

/* Responsive design */
@media (max-width: 768px) {
  .release-header h2 {
    font-size: 1.25rem;
  }
  
  .prerelease-badge {
    font-size: 0.7rem;
    padding: 0.2rem 0.4rem;
  }
  
  .changelog-footer {
    padding: 1rem;
  }
}
</style>