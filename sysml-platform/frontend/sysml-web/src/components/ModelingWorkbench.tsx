import React, { useState, useCallback } from 'react';
import { Box, AppBar, Toolbar, Typography, IconButton, Drawer, List, ListItem, ListItemIcon, ListItemText, Paper } from '@mui/material';
import { Menu as MenuIcon, FolderOpen, Save, Add, Settings, PlayArrow } from '@mui/icons-material';
import ReactFlow, { 
  Node, 
  Edge, 
  addEdge, 
  Background, 
  Controls, 
  MiniMap,
  useNodesState,
  useEdgesState,
  Connection,
  NodeTypes,
  MarkerType
} from 'reactflow';
import 'reactflow/dist/style.css';
import ModelTree from './ModelTree';
import PropertyPanel from './PropertyPanel';
import SysMLNode from './nodes/SysMLNode';

const nodeTypes: NodeTypes = {
  sysmlBlock: SysMLNode,
  sysmlRequirement: SysMLNode,
  sysmlPort: SysMLNode,
  sysmlAction: SysMLNode,
};

const initialNodes: Node[] = [];
const initialEdges: Edge[] = [];

const ModelingWorkbench: React.FC = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(true);
  const [nodeCounter, setNodeCounter] = useState(0);

  const onConnect = useCallback(
    (params: Connection) => {
      const newEdge = {
        ...params,
        markerEnd: {
          type: MarkerType.ArrowClosed,
        },
      };
      setEdges((eds) => addEdge(newEdge, eds));
    },
    [setEdges]
  );

  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
  }, []);

  const addNewNode = (type: string) => {
    const newNode: Node = {
      id: `${type}-${nodeCounter}`,
      type: `sysml${type}`,
      position: { x: 250 + (nodeCounter * 50) % 400, y: 100 + Math.floor(nodeCounter / 8) * 100 },
      data: { 
        label: `${type} ${nodeCounter}`,
        type: type,
        description: '',
      },
    };
    setNodes((nds) => nds.concat(newNode));
    setNodeCounter(nodeCounter + 1);
  };

  const saveModel = async () => {
    const modelData = {
      nodes,
      edges,
      timestamp: new Date().toISOString(),
    };
    console.log('Saving model to CDO:', modelData);
    // TODO: Call GraphQL mutation to save to CDO
    alert('Model saved to CDO repository');
  };

  const runSimulation = () => {
    console.log('Running SysML simulation...');
    alert('Simulation started');
  };

  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={() => setDrawerOpen(!drawerOpen)}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            SysML v2 建模平台 - CDO集成
          </Typography>
          <IconButton color="inherit" onClick={saveModel}>
            <Save />
          </IconButton>
          <IconButton color="inherit" onClick={runSimulation}>
            <PlayArrow />
          </IconButton>
          <IconButton color="inherit">
            <Settings />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="persistent"
        anchor="left"
        open={drawerOpen}
        sx={{
          width: 240,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: 240,
            boxSizing: 'border-box',
            top: '64px',
          },
        }}
      >
        <Box sx={{ overflow: 'auto', p: 2 }}>
          <Typography variant="h6" gutterBottom>
            元素面板
          </Typography>
          <List>
            <ListItem onClick={() => addNewNode('Block')}>
              <ListItemIcon><Add /></ListItemIcon>
              <ListItemText primary="Block" />
            </ListItem>
            <ListItem onClick={() => addNewNode('Requirement')}>
              <ListItemIcon><Add /></ListItemIcon>
              <ListItemText primary="Requirement" />
            </ListItem>
            <ListItem onClick={() => addNewNode('Port')}>
              <ListItemIcon><Add /></ListItemIcon>
              <ListItemText primary="Port" />
            </ListItem>
            <ListItem onClick={() => addNewNode('Action')}>
              <ListItemIcon><Add /></ListItemIcon>
              <ListItemText primary="Action" />
            </ListItem>
          </List>
          <Box sx={{ mt: 3 }}>
            <Typography variant="h6" gutterBottom>
              模型树
            </Typography>
            <ModelTree nodes={nodes} onSelectNode={setSelectedNode} />
          </Box>
        </Box>
      </Drawer>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 0,
          mt: '64px',
          ml: drawerOpen ? '240px' : 0,
          transition: 'margin 225ms cubic-bezier(0.4, 0, 0.6, 1) 0ms',
        }}
      >
        <Paper sx={{ height: 'calc(100vh - 64px)', display: 'flex' }}>
          <Box sx={{ flex: 1 }}>
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              onNodeClick={onNodeClick}
              nodeTypes={nodeTypes}
              fitView
            >
              <Background />
              <Controls />
              <MiniMap />
            </ReactFlow>
          </Box>
          {selectedNode && (
            <PropertyPanel 
              node={selectedNode} 
              onUpdateNode={(updatedNode) => {
                setNodes((nds) =>
                  nds.map((n) => (n.id === updatedNode.id ? updatedNode : n))
                );
              }}
            />
          )}
        </Paper>
      </Box>
    </Box>
  );
};

export default ModelingWorkbench;