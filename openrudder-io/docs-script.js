// Documentation page specific scripts

document.addEventListener('DOMContentLoaded', () => {
    // Highlight active section in sidebar
    const sections = document.querySelectorAll('.doc-section');
    const sidebarLinks = document.querySelectorAll('.sidebar-section a');
    
    const observerOptions = {
        threshold: 0.3,
        rootMargin: '-100px 0px -66% 0px'
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const id = entry.target.getAttribute('id');
                
                // Remove active class from all links
                sidebarLinks.forEach(link => link.classList.remove('active'));
                
                // Add active class to current link
                const activeLink = document.querySelector(`.sidebar-section a[href="#${id}"]`);
                if (activeLink) {
                    activeLink.classList.add('active');
                }
            }
        });
    }, observerOptions);
    
    sections.forEach(section => observer.observe(section));
    
    // Smooth scroll for sidebar links
    sidebarLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = link.getAttribute('href').substring(1);
            const targetSection = document.getElementById(targetId);
            
            if (targetSection) {
                const navHeight = document.querySelector('.navbar').offsetHeight;
                const targetPosition = targetSection.offsetTop - navHeight - 20;
                
                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });
            }
        });
    });
    
    // Copy code button functionality
    document.querySelectorAll('.code-block').forEach(block => {
        const button = document.createElement('button');
        button.className = 'copy-button';
        button.innerHTML = '📋 Copy';
        button.style.cssText = `
            position: absolute;
            top: 8px;
            right: 8px;
            padding: 4px 12px;
            background: rgba(59, 130, 246, 0.2);
            border: 1px solid rgba(59, 130, 246, 0.3);
            border-radius: 4px;
            color: var(--text-secondary);
            cursor: pointer;
            font-size: 0.75rem;
            transition: all 0.2s;
        `;
        
        block.style.position = 'relative';
        block.appendChild(button);
        
        button.addEventListener('click', () => {
            const code = block.querySelector('code').textContent;
            navigator.clipboard.writeText(code).then(() => {
                button.innerHTML = '✓ Copied!';
                button.style.background = 'rgba(16, 185, 129, 0.2)';
                button.style.borderColor = 'rgba(16, 185, 129, 0.3)';
                
                setTimeout(() => {
                    button.innerHTML = '📋 Copy';
                    button.style.background = 'rgba(59, 130, 246, 0.2)';
                    button.style.borderColor = 'rgba(59, 130, 246, 0.3)';
                }, 2000);
            });
        });
        
        button.addEventListener('mouseenter', () => {
            button.style.background = 'rgba(59, 130, 246, 0.3)';
        });
        
        button.addEventListener('mouseleave', () => {
            if (button.innerHTML !== '✓ Copied!') {
                button.style.background = 'rgba(59, 130, 246, 0.2)';
            }
        });
    });
});
