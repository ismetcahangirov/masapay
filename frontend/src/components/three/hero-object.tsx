import { useRef } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import type { Mesh } from 'three'

// Minimal geometric accent: a near-black wireframe icosahedron that rotates
// slowly. meshBasicMaterial needs no lighting. Kept deliberately simple to stay
// on the minimalist brief and keep the frame cost low.
function Shape({ animate }: { animate: boolean }) {
  const ref = useRef<Mesh>(null)

  useFrame((_, delta) => {
    if (!animate || !ref.current) return
    ref.current.rotation.x += delta * 0.15
    ref.current.rotation.y += delta * 0.2
  })

  return (
    <mesh ref={ref}>
      <icosahedronGeometry args={[1.6, 1]} />
      <meshBasicMaterial color="#0a0a0a" wireframe />
    </mesh>
  )
}

// Default export so it can be React.lazy-loaded — this pulls three.js into its
// own chunk, out of the initial bundle.
export default function HeroObject({ animate = true }: { animate?: boolean }) {
  return (
    <Canvas
      camera={{ position: [0, 0, 5], fov: 40 }}
      frameloop={animate ? 'always' : 'demand'}
      dpr={[1, 1.5]}
      gl={{ antialias: true, alpha: true }}
      style={{ background: 'transparent' }}
    >
      <Shape animate={animate} />
    </Canvas>
  )
}
