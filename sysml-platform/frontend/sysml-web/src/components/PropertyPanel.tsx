import React, { useState, useEffect } from 'react';
import { Box, TextField, Typography, Paper, Button } from '@mui/material';
import { Node } from 'reactflow';

interface PropertyPanelProps {
  node: Node;
  onUpdateNode: (node: Node) => void;
}

const PropertyPanel: React.FC<PropertyPanelProps> = ({ node, onUpdateNode }) => {
  const [label, setLabel] = useState(node.data.label);
  const [description, setDescription] = useState(node.data.description || '');

  useEffect(() => {
    setLabel(node.data.label);
    setDescription(node.data.description || '');
  }, [node]);

  const handleSave = () => {
    onUpdateNode({
      ...node,
      data: {
        ...node.data,
        label,
        description,
      },
    });
  };

  return (
    <Paper sx={{ width: 300, p: 2, height: '100%', overflowY: 'auto' }}>
      <Typography variant="h6" gutterBottom>
        属性面板
      </Typography>
      
      <Box sx={{ mb: 2 }}>
        <Typography variant="subtitle2" color="textSecondary">
          类型: {node.data.type}
        </Typography>
        <Typography variant="caption" color="textSecondary">
          ID: {node.id}
        </Typography>
      </Box>

      <TextField
        fullWidth
        label="名称"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
        margin="normal"
        variant="outlined"
      />

      <TextField
        fullWidth
        label="描述"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        margin="normal"
        variant="outlined"
        multiline
        rows={4}
      />

      <Box sx={{ mt: 2 }}>
        <Button 
          variant="contained" 
          color="primary" 
          fullWidth
          onClick={handleSave}
        >
          更新属性
        </Button>
      </Box>

      <Box sx={{ mt: 3 }}>
        <Typography variant="subtitle2" gutterBottom>
          CDO状态
        </Typography>
        <Typography variant="caption" color="success.main">
          ✓ 已连接到CDO仓库
        </Typography>
      </Box>
    </Paper>
  );
};

export default PropertyPanel;