// Multi-Tenant Document Search Frontend
// All API calls include X-Tenant-Id header from the input box
// Handles document creation, search, detail view, deletion, pagination, and health check

const apiBase = '';
let currentPage = 0;
let pageSize = 10;
let lastQuery = '';

// Utility: Get tenant ID from input
function getTenantId() {
    return document.getElementById('tenantId').value.trim();
}

// Utility: Set loading state for a section
function setLoading(sectionId, isLoading) {
    document.getElementById(sectionId).style.display = isLoading ? '' : 'none';
}

// Utility: Show message
function showMsg(id, msg, type) {
    const el = document.getElementById(id);
    el.textContent = msg;
    el.className = 'msg' + (type ? ' ' + type : '');
}

// Health check fetch and display
async function fetchHealth() {
    try {
        const res = await fetch(apiBase + '/actuator/health');
        if (!res.ok) throw new Error('Health check failed');
        const data = await res.json();
        document.getElementById('healthStatus').textContent =
            data.status === 'UP' ? 'Backend: Healthy' : 'Backend: ' + data.status;
        document.getElementById('healthStatus').style.color = data.status === 'UP' ? '#388e3c' : '#d32f2f';
    } catch (e) {
        document.getElementById('healthStatus').textContent = 'Backend: Unreachable';
        document.getElementById('healthStatus').style.color = '#d32f2f';
    }
}

// Add document handler
async function handleAddDoc(e) {
    e.preventDefault();
    showMsg('addDocMsg', '', '');
    setLoading('addDocLoading', true);
    const tenantId = getTenantId();
    if (!tenantId) {
        showMsg('addDocMsg', 'Please enter Tenant ID.', 'error');
        setLoading('addDocLoading', false);
        return;
    }
    const title = document.getElementById('docTitle').value.trim();
    const content = document.getElementById('docContent').value.trim();
    try {
        const res = await fetch(apiBase + '/documents', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Tenant-Id': tenantId
            },
            body: JSON.stringify({ title, content })
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Failed to add document');
        }
        showMsg('addDocMsg', 'Document added successfully!', 'success');
        document.getElementById('addDocForm').reset();
    } catch (e) {
        showMsg('addDocMsg', e.message, 'error');
    } finally {
        setLoading('addDocLoading', false);
    }
}

// Search handler
async function handleSearch(e, page = 0) {
    if (e) e.preventDefault();
    showMsg('searchMsg', '', '');
    setLoading('searchLoading', true);
    const tenantId = getTenantId();
    if (!tenantId) {
        showMsg('searchMsg', 'Please enter Tenant ID.', 'error');
        setLoading('searchLoading', false);
        return;
    }
    const query = document.getElementById('searchQuery').value.trim();
    if (!query) {
        showMsg('searchMsg', 'Please enter a search query.', 'error');
        setLoading('searchLoading', false);
        return;
    }
    lastQuery = query;
    currentPage = page;
    try {
        const res = await fetch(apiBase + `/search?q=${encodeURIComponent(query)}&page=${page}&size=${pageSize}`, {
            headers: { 'X-Tenant-Id': tenantId }
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Search failed');
        }
        const data = await res.json();
        renderResults(data);
    } catch (e) {
        showMsg('searchMsg', e.message, 'error');
        document.getElementById('results').innerHTML = '';
        document.getElementById('pagination').innerHTML = '';
    } finally {
        setLoading('searchLoading', false);
    }
}

// Render search results and pagination
function renderResults(data) {
    if (!data || !data.content || data.content.length === 0) {
        document.getElementById('results').innerHTML = '<div>No results found.</div>';
        document.getElementById('pagination').innerHTML = '';
        return;
    }
    let html = '<table class="table"><thead><tr><th>Title</th><th>Snippet</th><th>Created</th><th>Delete</th></tr></thead><tbody>';
    for (const doc of data.content) {
        html += `<tr data-id="${doc.id}">
            <td>${escapeHtml(doc.title)}</td>
            <td>${escapeHtml((doc.content || '').slice(0, 60))}...</td>
            <td>${doc.createdAt ? new Date(doc.createdAt).toLocaleString() : ''}</td>
            <td><button class="delete-btn" data-id="${doc.id}">Delete</button></td>
        </tr>`;
    }
    html += '</tbody></table>';
    document.getElementById('results').innerHTML = html;
    // Pagination
    let pagHtml = '';
    for (let i = 0; i < data.totalPages; i++) {
        pagHtml += `<button class="${i === data.number ? 'active' : ''}" onclick="goToPage(${i})">${i + 1}</button>`;
    }
    document.getElementById('pagination').innerHTML = pagHtml;
    // Row click for detail
    document.querySelectorAll('#results tr[data-id]').forEach(row => {
        row.addEventListener('click', function (e) {
            if (e.target.classList.contains('delete-btn')) return;
            fetchDocDetail(this.getAttribute('data-id'));
        });
    });
    // Delete button
    document.querySelectorAll('.delete-btn').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            deleteDocument(this.getAttribute('data-id'));
        });
    });
}

// Go to page
window.goToPage = function(page) {
    handleSearch(null, page);
};

// Fetch and show document detail
async function fetchDocDetail(id) {
    const tenantId = getTenantId();
    if (!tenantId) return;
    setModal(true, '<div class="loading">Loading...</div>');
    try {
        const res = await fetch(apiBase + `/documents/${id}`, {
            headers: { 'X-Tenant-Id': tenantId }
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Failed to fetch document');
        }
        const doc = await res.json();
        setModal(true, `<h3>${escapeHtml(doc.title)}</h3>
            <div><b>Created:</b> ${doc.createdAt ? new Date(doc.createdAt).toLocaleString() : ''}</div>
            <hr><div>${escapeHtml(doc.content)}</div>`);
    } catch (e) {
        setModal(true, `<div class="msg error">${e.message}</div>`);
    }
}

// Delete document
async function deleteDocument(id) {
    if (!confirm('Delete this document?')) return;
    const tenantId = getTenantId();
    if (!tenantId) return;
    try {
        const res = await fetch(apiBase + `/documents/${id}`, {
            method: 'DELETE',
            headers: { 'X-Tenant-Id': tenantId }
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Delete failed');
        }
        showMsg('searchMsg', 'Document deleted.', 'success');
        handleSearch(null, currentPage);
    } catch (e) {
        showMsg('searchMsg', e.message, 'error');
    }
}

// Modal show/hide
function setModal(show, html) {
    const modal = document.getElementById('docDetailModal');
    if (show) {
        document.getElementById('docDetail').innerHTML = html;
        modal.style.display = '';
    } else {
        modal.style.display = 'none';
    }
}
document.getElementById('closeDetail').onclick = () => setModal(false);
window.onclick = function(event) {
    const modal = document.getElementById('docDetailModal');
    if (event.target === modal) setModal(false);
};

// Escape HTML utility
function escapeHtml(str) {
    return (str || '').replace(/[&<>"']/g, function (c) {
        return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[c];
    });
}

// Event listeners
fetchHealth();
document.getElementById('addDocForm').addEventListener('submit', handleAddDoc);
document.getElementById('searchForm').addEventListener('submit', handleSearch);
// Refresh health every 30s
setInterval(fetchHealth, 30000);

// Comments in code:
// - Tenant ID is always read from the input and sent as X-Tenant-Id header in all API calls.
// - Search queries are sent via GET /search, results are rendered in a table with pagination.
// - Document creation uses POST /documents, deletion uses DELETE /documents/{id}.
// - Loading indicators and error/success messages are shown for all operations.
// - Health check is fetched from /actuator/health and displayed at the top.
// - UI state (modals, forms, pagination) is managed in JS for a smooth UX.

