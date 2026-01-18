# Workflow Diagram Feature

## Overview

Added an interactive workflow diagram visualization that displays when the user selects the "Leave Request Workflow". The diagram appears between the workflow selector and the input section.

## Visual Representation

When "Leave Request Workflow" is selected, users will see:

```
┌─────────────────────────────────────────────────┐
│           📊 Workflow Diagram                   │
│    Leave Request Approval Workflow              │
├─────────────────────────────────────────────────┤
│                                                 │
│              ┌─────────────┐                    │
│              │   🚀 Start   │                   │
│              │  Initialize  │                   │
│              └──────┬───────┘                   │
│                     ↓                           │
│              ┌─────────────┐                    │
│              │ 🤖 Parse     │                   │
│              │  Request     │                   │
│              │ Extract data │                   │
│              └──────┬───────┘                   │
│                     ↓                           │
│              ┌─────────────┐                    │
│              │ 🧠 AI        │                   │
│              │ Validation   │                   │
│              │ Evaluate     │                   │
│              └──────┬───────┘                   │
│                     ↓                           │
│              ┌─────────────┐                    │
│              │ 🔀 Decision  │                   │
│              │   Router     │                   │
│              └──────┬───────┘                   │
│                     │                           │
│         ┌───────────┼───────────┐               │
│         │           │           │               │
│         ↓           ↓           ↓               │
│   ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│   │✅ High  │ │⚠️ Low   │ │❌ High  │          │
│   │Confidence│ │Confidence│ │Confidence│        │
│   │ APPROVE │ │ESCALATE │ │ REJECT  │          │
│   └────┬────┘ └────┬────┘ └────┬────┘          │
│        ↓           ↓           ↓               │
│   ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│   │👍 Auto  │ │👤 Human │ │🚫 Auto  │          │
│   │ Approve │ │ Review  │ │ Reject  │          │
│   └────┬────┘ └────┬────┘ └────┬────┘          │
│        └───────┬────┴───────┘               │
│                ↓                               │
│         ┌─────────────┐                        │
│         │   🏁 End     │                       │
│         │  Complete    │                       │
│         └─────────────┘                        │
│                                                 │
├─────────────────────────────────────────────────┤
│  Legend:                                        │
│  🔵 Start/End  🔷 Process  🟡 AI Decision       │
│  🟢 Approve    🟣 Review   🔴 Reject             │
└─────────────────────────────────────────────────┘
```

## Features

### 1. **Visual Node Types**

Each node type has:
- **Unique color gradient** for easy identification
- **Icon** representing its function
- **Label** with the node name
- **Description** explaining what it does
- **Hover effect** with shadow and lift

### 2. **Node Color Coding**

- **Purple Gradient** (Start/End): Entry and exit points
- **Blue Gradient** (Parse): Data processing
- **Pink-Yellow Gradient** (AI Validation): AI-powered nodes
- **Cyan Gradient** (Decision Router): Routing logic
- **Green Gradient** (Auto Approve): Success path
- **Pink Gradient** (Human Review): Manual intervention
- **Red Gradient** (Auto Reject): Rejection path

### 3. **Decision Branches**

Three clear paths with labeled conditions:
- **✅ High Confidence APPROVE (≥85%)**: Green badge, auto-approval
- **⚠️ Low Confidence or ESCALATE**: Yellow badge, human review
- **❌ High Confidence REJECT (≥85%)**: Red badge, auto-rejection

### 4. **Interactive Legend**

Color-coded legend at the bottom showing:
- Node type meanings
- Visual indicators
- Quick reference guide

### 5. **Responsive Design**

- Desktop: Three-column branch layout
- Mobile: Single-column stacked layout
- Arrows adjust automatically

## Implementation Details

### Component: WorkflowDiagram.vue

**Location**: `frontend/src/components/WorkflowDiagram.vue`

**Props**:
- `workflowType`: string - The selected workflow type

**Logic**:
- Conditionally renders diagram based on workflow type
- Currently implements "leave_request" workflow
- Easily extensible for additional workflow types

### Integration

**In App.vue**:
```vue
<!-- Workflow Diagram -->
<WorkflowDiagram 
  v-if="selectedWorkflowType" 
  :workflowType="selectedWorkflowType" 
/>
```

The diagram appears:
1. After workflow type selection
2. Before the user input section
3. Above execution results

## User Experience Flow

1. **User arrives** → Sees workflow selector
2. **Selects "Leave Request Workflow"** → Diagram immediately appears
3. **Reads diagram** → Understands the workflow logic
4. **Enters input** → Executes workflow
5. **Views results** → Compares execution trace with diagram

## Benefits

### Educational
- **Visual Learning**: Users understand the workflow before executing
- **Decision Logic**: Clear visualization of AI decision points
- **Confidence Thresholds**: Shows exactly when auto-approval/rejection occurs

### Debugging
- **Reference Point**: Compare execution trace with expected flow
- **Path Understanding**: See which branch was taken
- **Troubleshooting**: Identify where workflow deviated

### Documentation
- **Self-Documenting**: Workflow is visually explained
- **No External Tools**: Built-in visualization
- **Always Up-to-Date**: Reflects actual implementation

## Extensibility

### Adding New Workflows

To add a diagram for a new workflow type:

```vue
<div v-if="workflowType === 'expense_approval'" class="diagram-container">
  <!-- Add your custom workflow diagram here -->
  <div class="flow-node start-node">...</div>
  <!-- ... more nodes ... -->
</div>
```

### Customization Options

1. **Colors**: Modify gradient styles in component CSS
2. **Icons**: Change emoji icons to match your theme
3. **Layout**: Adjust node spacing and arrangement
4. **Animations**: Add entrance animations for nodes

## Technical Details

### CSS Features
- Flexbox layout for vertical flow
- CSS Grid for branch layout
- Linear gradients for visual appeal
- Box shadows for depth
- Smooth transitions on hover
- Responsive breakpoints for mobile

### Accessibility
- Clear visual hierarchy
- High contrast colors
- Readable font sizes
- Hover states for interaction
- Semantic HTML structure

## Future Enhancements

Potential improvements:
- [ ] Animated flow showing execution path
- [ ] Highlight active node during execution
- [ ] Click nodes for detailed information
- [ ] Export diagram as image
- [ ] Zoom and pan for complex workflows
- [ ] Mermaid.js integration for dynamic diagrams
- [ ] D3.js for advanced visualizations

## Example Screenshots

### Desktop View
- Full three-column branch display
- All nodes visible at once
- Legend at bottom
- Clean, professional appearance

### Mobile View
- Single column layout
- Vertical scrolling
- Tap-friendly node sizes
- Simplified arrows

## Summary

The workflow diagram feature enhances the user experience by:
✅ Providing immediate visual understanding
✅ Showing AI decision logic clearly
✅ Helping users understand confidence thresholds
✅ Serving as built-in documentation
✅ Improving debugging and troubleshooting

Users can now see exactly how their request will be processed before execution, making the workflow engine more transparent and user-friendly.

