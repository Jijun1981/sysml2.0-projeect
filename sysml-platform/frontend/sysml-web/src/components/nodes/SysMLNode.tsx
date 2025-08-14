import React from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Paper, Typography, Box } from '@mui/material';

const SysMLNode: React.FC<NodeProps> = ({ data, selected }) => {
  const getNodeColor = (type: string) => {
    switch (type) {
      case 'Block':
        return '#3498db';
      case 'Requirement':
        return '#e74c3c';
      case 'Port':
        return '#f39c12';
      case 'Action':
        return '#27ae60';
      default:
        return '#95a5a6';
    }
  };

  return (
    <Paper
      elevation={selected ? 6 : 2}
      sx={{
        padding: 2,
        backgroundColor: getNodeColor(data.type),
        color: 'white',
        minWidth: 120,
        minHeight: 60,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        border: selected ? '2px solid #333' : 'none',
      }}
    >
      <Handle type="target" position={Position.Top} />
      <Box>
        <Typography variant="subtitle2" sx={{ opacity: 0.8 }}>
          «{data.type}»
        </Typography>
        <Typography variant="body1" fontWeight="bold">
          {data.label}
        </Typography>
        {data.description && (
          <Typography variant="caption" sx={{ opacity: 0.9, mt: 1 }}>
            {data.description}
          </Typography>
        )}
      </Box>
      <Handle type="source" position={Position.Bottom} />
    </Paper>
  );
};

export default SysMLNode;