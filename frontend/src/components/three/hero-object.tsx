import { useLayoutEffect, useMemo, useRef } from 'react'
import { Canvas, useFrame, type ThreeEvent } from '@react-three/fiber'
import {
  Color,
  CylinderGeometry,
  Group,
  IcosahedronGeometry,
  InstancedMesh,
  Mesh,
  MeshBasicMaterial,
  Object3D,
  Vector3,
} from 'three'

const UP = new Vector3(0, 1, 0)
const NEAR_BLACK = new Color('#0a0a0a')
const WIRE_GREEN = new Color('#08cb00')
const tmpColor = new Color()
const targetColor = new Color()

type Edge = { a: Vector3; b: Vector3 }

// Unique edges of an icosahedron. The geometry is non-indexed, so edges are
// de-duplicated by rounded endpoint position.
function buildEdges(radius: number, detail: number): Edge[] {
  const geo = new IcosahedronGeometry(radius, detail)
  const pos = geo.attributes.position
  const seen = new Set<string>()
  const edges: Edge[] = []
  const v = [new Vector3(), new Vector3(), new Vector3()]
  const key = (p: Vector3) =>
    `${Math.round(p.x * 1000)},${Math.round(p.y * 1000)},${Math.round(p.z * 1000)}`

  for (let i = 0; i < pos.count; i += 3) {
    v[0].fromBufferAttribute(pos, i)
    v[1].fromBufferAttribute(pos, i + 1)
    v[2].fromBufferAttribute(pos, i + 2)
    for (const [x, y] of [
      [0, 1],
      [1, 2],
      [2, 0],
    ] as const) {
      const ka = key(v[x])
      const kb = key(v[y])
      const k = ka < kb ? `${ka}|${kb}` : `${kb}|${ka}`
      if (seen.has(k)) continue
      seen.add(k)
      edges.push({ a: v[x].clone(), b: v[y].clone() })
    }
  }
  geo.dispose()
  return edges
}

function Figure({ animate }: { animate: boolean }) {
  const groupRef = useRef<Group>(null)
  const meshRef = useRef<InstancedMesh>(null)
  const sparkRef = useRef<Mesh>(null)
  const hoveredId = useRef(-1)

  const { geometry, material, edges } = useMemo(() => {
    const edges = buildEdges(1.6, 1)
    // Thin, open-ended cylinders read as wires but still raycast for hover.
    const geometry = new CylinderGeometry(0.01, 0.01, 1, 6, 1, true)
    const material = new MeshBasicMaterial({
      color: 0xffffff,
      toneMapped: false,
    })
    return { geometry, material, edges }
  }, [])
  const count = edges.length

  // Place one cylinder per edge and seed every instance to the base colour.
  useLayoutEffect(() => {
    const inst = meshRef.current
    if (!inst) return
    const dummy = new Object3D()
    const dir = new Vector3()
    edges.forEach((e, i) => {
      dir.subVectors(e.b, e.a)
      const len = dir.length()
      dummy.position.copy(e.a).add(e.b).multiplyScalar(0.5)
      dummy.quaternion.setFromUnitVectors(UP, dir.clone().normalize())
      dummy.scale.set(1, len, 1)
      dummy.updateMatrix()
      inst.setMatrixAt(i, dummy.matrix)
      inst.setColorAt(i, NEAR_BLACK)
    })
    inst.instanceMatrix.needsUpdate = true
    if (inst.instanceColor) inst.instanceColor.needsUpdate = true
  }, [edges])

  useFrame((state, delta) => {
    if (animate && groupRef.current) {
      groupRef.current.rotation.x += delta * 0.15
      groupRef.current.rotation.y += delta * 0.2
    }

    const inst = meshRef.current
    const colors = inst?.instanceColor
    if (inst && colors) {
      const t = state.clock.elapsedTime
      const hid = hoveredId.current
      const lerp = animate ? Math.min(1, delta * 14) : 1
      for (let i = 0; i < count; i++) {
        if (i === hid) {
          // Electric buzz: fast, uneven green flicker.
          const flick =
            0.55 +
            0.45 * Math.abs(Math.sin(t * 26 + i * 1.3)) +
            0.25 * Math.sin(t * 90 + i)
          targetColor.copy(WIRE_GREEN).multiplyScalar(Math.max(0.4, flick))
        } else {
          targetColor.copy(NEAR_BLACK)
        }
        tmpColor.fromArray(colors.array as Float32Array, i * 3)
        tmpColor.lerp(targetColor, lerp)
        tmpColor.toArray(colors.array as Float32Array, i * 3)
      }
      colors.needsUpdate = true
    }

    // A bright spark travelling along the hovered edge = current flowing.
    const spark = sparkRef.current
    if (spark) {
      const hid = hoveredId.current
      if (hid >= 0 && hid < count && animate) {
        const e = edges[hid]
        const u = (state.clock.elapsedTime * 0.9) % 1
        spark.position.lerpVectors(e.a, e.b, u)
        spark.visible = true
      } else {
        spark.visible = false
      }
    }
  })

  return (
    <group ref={groupRef}>
      <instancedMesh
        ref={meshRef}
        args={[geometry, material, count]}
        onPointerMove={(event: ThreeEvent<PointerEvent>) => {
          event.stopPropagation()
          hoveredId.current = event.instanceId ?? -1
        }}
        onPointerOut={() => {
          hoveredId.current = -1
        }}
      />
      <mesh ref={sparkRef} visible={false}>
        <sphereGeometry args={[0.05, 14, 14]} />
        <meshBasicMaterial color="#8bff9e" toneMapped={false} />
      </mesh>
    </group>
  )
}

// Default export so it can be React.lazy-loaded — this pulls three.js into its
// own chunk, out of the initial bundle.
export default function HeroObject({ animate = true }: { animate?: boolean }) {
  return (
    <Canvas
      camera={{ position: [0, 0, 5], fov: 40 }}
      dpr={[1, 1.5]}
      gl={{ antialias: true, alpha: true }}
      style={{ background: 'transparent' }}
    >
      <Figure animate={animate} />
    </Canvas>
  )
}
