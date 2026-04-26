# Enchiridion v2 + LevelRPG
## Architecture Source of Truth

This document defines the canonical architecture, rendering model, and interaction system for the Enchiridion book system and its LevelRPG integration.

Any changes made by AI or developers MUST conform to this document.

---

# 1. CORE PRINCIPLES

## 1.1 Single Rendering Truth
- ALL page content is rendered into render targets (textures)
- These textures are applied to the GeckoLib book model
- There are NO screen-space UI elements for page content

❌ DO NOT:
- Render buttons using screen-space overlays
❌ Add screen-space UI for page content (things that belong inside the book's texture pipeline) 


✅ DO:
- Render ALL interactive elements into page textures
- Use page-local coordinates for interaction
  HUD elements for states that operate outside the page pipeline (reel, cover overlays) are allowed as screen-space

---

## 1.2 Page-Native Interaction Model
- All interaction originates from page content
- Interactive elements are defined during layout
- Hit detection is resolved via projected page geometry

Pipeline:
1. Layout defines element bounds
2. Bounds are rendered into texture
3. Same bounds are used for hit detection

This MUST remain synchronized.

---

## 1.3 No Duplicate Interaction Systems
There is ONLY ONE interaction system:

✔ BookInteractionResolver  
✔ PageInteractiveNode  
✔ ResolvedBookInteractionTarget  

❌ DO NOT:
- Introduce new interaction systems
- Use screen-space bounding boxes
- Create ad-hoc click handlers

---

# 2. RENDERING PIPELINE

## 2.1 Flow

BookPage → PageCanvasRenderer → RenderTarget → GeckoLib Model → Screen

## 2.2 Rules

- PageCanvasRenderer is the ONLY place that draws page content
- BookSceneRenderer handles ONLY:
  - Model transforms
  - Presentation
  - Projection math

- BookOverlayRenderer is ONLY for:
  - Debug overlays
  - Tooltips
  - Projection overlays (NOT page UI)

---

## 2.3 Left Page Mirroring

- Left page textures are mirrored at render time
- UV → local mapping compensates for this

❌ NEVER:
- Apply additional mirroring elsewhere
- Flip coordinates downstream

---

# 3. BOOK STRUCTURE

## 3.1 Spread Model

- Spread = Left Page + Right Page
- Index 0 = Virtual Front Cover

### Front Cover Behavior
- NOT a real page
- NOT part of document spreads
- Provided virtually by BookProvider

Front Cover:
- Left = physical cover surface
- Right = tracked page with text support- should have the book's title in the middle

---

## 3.2 Provider Rules

LevelRpgJournalBookProvider:

- Offsets spreads by +1
- Offsets pages by +2

This MUST remain consistent or navigation breaks.

---

# 4. PAGE LAYOUT SYSTEM

## 4.1 Slot-Based Layout

Each page uses structured slots:

- TITLE
- SUBTITLE
- FOCAL
- BODY
- STATS
- FOOTER
- INTERACTION

## 4.2 Rules

- Layout is declarative
- Renderer does NOT reposition elements
- Slots define ALL positioning

---

## 4.3 Text Behavior

- BODY wraps
- SHORT text scales-to-fit
- No uncontrolled overflow

---

# 5. INTERACTIVE ELEMENTS

## 5.1 Element Types

- TextElement
- InteractiveTextElement
- ButtonElement
- DecorationElement

## 5.2 Rules

- ALL elements render into page texture
- Hover state is baked into texture
- No separate hover UI

---

# 6. SKILL SYSTEM (LevelRPG)

## 6.1 Skills

Seven skills. See `LevelRPG/SKILL_DESIGN.md` for full identity and philosophy of each.

- **Valor** — Combat through power. Two branches: Offense and Grit (absorbs former Vitality).
- **Finesse** — Combat through mobility and positioning. Bows as natural expression, not core identity.
- **Arcana** — Scholarly understanding of Minecraft's magical forces. Multiplier across other skills.
- **Delving** — Mastery of the hidden and submerged (underground + underwater).
- **Forging** — Blacksmith identity. Signed works, special properties, social weight.
- **Artificing** — Making things that persist. Two branches: Architect and Mechanist.
- **Hearth** — Food, brewing, alchemy, sustenance. The caretaker skill.

---

## 6.2 Progression Model

Two-track system:

### Mastery
- Passive
- Earned by doing actions

### Skill Level
- Active
- Spent via skill points

---

## 6.3 UI Rules

Skill Page MUST include:
- Large centered level
- Mastery progress
- Description
- "Open Skill" interaction

---

# 7. PROJECTION SYSTEM

## 7.1 States

- JOURNAL_READING
- SKILL_PROJECTION_TRANSITION
- SKILL_PROJECTION_ACTIVE

## 7.2 Rules

- Projection is NOT a new screen
- It is an overlay state

---

# 8. ARCHETYPE SYSTEM

## 8.1 Front Cover Interaction

- Archetype selection happens on front cover
- Hit region attaches to cover surface (NOT page)

## 8.2 Card Reel Requirements

- Cards are 3D objects
- Must be spatially distributed (not stacked)
- Center card is focal
- Side cards are offset

❌ DO NOT:
- Move only in depth (Z)
- Attach UI to moving cards

## 8.3 Reel Info Panel

Static screen-space HUD element
Positioned in lower viewport, never attached to card positions
Updates only when reel phase is IDLE (settled focus)
Navigation hints render relative to this panel, never relative to cards

---

# 9. ANIMATION RULES

## 9.1 Book States

All animation states MUST respond to ESC within one tick, either by completing immediately or transitioning to a dismissable state. 
Players must never be trapped in a non-cancellable animation.

## 9.2 Critical Rule

Animations MUST NOT:
- Snap instantly
- Override authored timing

---

# 10. WHAT AI MUST NEVER DO

❌ Add screen-space UI for book content  
❌ Duplicate interaction logic  
❌ Modify projection math casually  
❌ Break spread indexing  
❌ Attach UI to animated transforms incorrectly  
❌ Introduce new rendering paths  
❌ Modify rightSurfaceTargetFor to add renderTexture = true for IDLE_FRONT — the right bone is captured for position only, 
painting page content on it is incorrect in front-cover state.

---

# 11. WHAT AI SHOULD DO

✔ Work within PageCanvasRenderer  
✔ Use existing layout slots  
✔ Maintain page-local coordinate system  
✔ Keep interaction + rendering unified  
✔ Respect provider offsets  

---

# 12. CURRENT KNOWN ISSUES

(Keep this updated)

Editor mode translates elements in wrong direction on left pages
Stat ledger interaction boxes mirrored horizontally
Stat ledger spills to 2 pages with clipping "Folio" text
Debug UI elements printing in upper-left of page contents
No ESC-to-cancel during animations
Snap artifacts entering/exiting front and back cover states
No exit animation from front or back cover
Front cover right page title overlay not rendering correctly

---