import React, { useEffect, useRef } from 'react';

const AnimatedBackground = () => {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let animationFrameId;
    let particles = [];

    // Set canvas size
    const resizeCanvas = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);

    // Calculate form card boundaries (center of screen, approximate size)
    const getFormBounds = () => {
      const formWidth = Math.min(700, canvas.width - 40); // maxWidth 700px + padding
      const formHeight = canvas.height * 0.8; // approximate height
      return {
        left: (canvas.width - formWidth) / 2,
        right: (canvas.width + formWidth) / 2,
        top: (canvas.height - formHeight) / 2,
        bottom: (canvas.height + formHeight) / 2
      };
    };

    // Particle class
    class Particle {
      constructor() {
        this.x = Math.random() * canvas.width;
        this.y = Math.random() * canvas.height;
        this.size = Math.random() * 3 + 1;
        this.speedX = Math.random() * 0.5 - 0.25;
        this.speedY = Math.random() * 0.5 - 0.25;
        this.opacity = Math.random() * 0.5 + 0.3;
      }

      update() {
        this.x += this.speedX;
        this.y += this.speedY;

        const form = getFormBounds();
        const padding = 10; // Extra padding for collision

        // Check collision with form card
        if (
          this.x + this.size > form.left - padding &&
          this.x - this.size < form.right + padding &&
          this.y + this.size > form.top - padding &&
          this.y - this.size < form.bottom + padding
        ) {
          // Determine which side we hit
          const distLeft = Math.abs(this.x - form.left);
          const distRight = Math.abs(this.x - form.right);
          const distTop = Math.abs(this.y - form.top);
          const distBottom = Math.abs(this.y - form.bottom);

          const minDist = Math.min(distLeft, distRight, distTop, distBottom);

          // Bounce off the closest side
          if (minDist === distLeft || minDist === distRight) {
            this.speedX *= -1;
            // Push particle out of collision
            if (minDist === distLeft) this.x = form.left - padding - this.size;
            else this.x = form.right + padding + this.size;
          } else {
            this.speedY *= -1;
            // Push particle out of collision
            if (minDist === distTop) this.y = form.top - padding - this.size;
            else this.y = form.bottom + padding + this.size;
          }
        }

        // Wrap around edges
        if (this.x > canvas.width) this.x = 0;
        if (this.x < 0) this.x = canvas.width;
        if (this.y > canvas.height) this.y = 0;
        if (this.y < 0) this.y = canvas.height;
      }

      draw() {
        ctx.fillStyle = `rgba(255, 107, 53, ${this.opacity})`;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // Create particles
    const particleCount = 80;
    for (let i = 0; i < particleCount; i++) {
      particles.push(new Particle());
    }

    // Draw connections between nearby particles
    const connectParticles = () => {
      const maxDistance = 150;
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const distance = Math.sqrt(dx * dx + dy * dy);

          if (distance < maxDistance) {
            const opacity = (1 - distance / maxDistance) * 0.3;
            ctx.strokeStyle = `rgba(255, 140, 66, ${opacity})`;
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.stroke();
          }
        }
      }
    };

    // Animation loop
    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      // Update and draw particles
      particles.forEach(particle => {
        particle.update();
        particle.draw();
      });

      // Draw connections
      connectParticles();

      animationFrameId = requestAnimationFrame(animate);
    };

    animate();

    // Cleanup
    return () => {
      window.removeEventListener('resize', resizeCanvas);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <>
      <canvas
        ref={canvasRef}
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          zIndex: 0,
          pointerEvents: 'none'
        }}
      />

      {/* Floating gradient orbs */}
      <style>
        {`
          @keyframes float-1 {
            0%, 100% {
              transform: translate(0, 0) scale(1);
            }
            33% {
              transform: translate(30px, -50px) scale(1.1);
            }
            66% {
              transform: translate(-20px, -30px) scale(0.9);
            }
          }

          @keyframes float-2 {
            0%, 100% {
              transform: translate(0, 0) scale(1);
            }
            33% {
              transform: translate(-40px, 40px) scale(1.15);
            }
            66% {
              transform: translate(20px, -20px) scale(0.95);
            }
          }

          @keyframes float-3 {
            0%, 100% {
              transform: translate(0, 0) scale(1);
            }
            33% {
              transform: translate(25px, 35px) scale(1.05);
            }
            66% {
              transform: translate(-30px, 20px) scale(1.1);
            }
          }

          .gradient-orb {
            position: absolute;
            border-radius: 50%;
            filter: blur(40px);
            opacity: 0.6;
            pointer-events: none;
          }
        `}
      </style>

      <div
        className="gradient-orb"
        style={{
          width: '300px',
          height: '300px',
          background: 'radial-gradient(circle, rgba(255, 140, 66, 0.8) 0%, rgba(255, 107, 53, 0) 70%)',
          top: '10%',
          left: '15%',
          animation: 'float-1 20s ease-in-out infinite'
        }}
      />
      <div
        className="gradient-orb"
        style={{
          width: '400px',
          height: '400px',
          background: 'radial-gradient(circle, rgba(255, 107, 53, 0.6) 0%, rgba(255, 140, 66, 0) 70%)',
          bottom: '15%',
          right: '10%',
          animation: 'float-2 25s ease-in-out infinite'
        }}
      />
      <div
        className="gradient-orb"
        style={{
          width: '250px',
          height: '250px',
          background: 'radial-gradient(circle, rgba(255, 180, 120, 0.7) 0%, rgba(255, 140, 66, 0) 70%)',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          animation: 'float-3 18s ease-in-out infinite'
        }}
      />
    </>
  );
};

export default AnimatedBackground;

