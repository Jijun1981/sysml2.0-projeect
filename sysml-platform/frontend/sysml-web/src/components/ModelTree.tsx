import React from 'react';
import { TreeView } from '@mui/x-tree-view/TreeView';
import { TreeItem } from '@mui/x-tree-view/TreeItem';
import { ExpandMore, ChevronRight, Folder, Description } from '@mui/icons-material';
import { Node } from 'reactflow';

interface ModelTreeProps {
  nodes: Node[];
  onSelectNode: (node: Node) => void;
}

const ModelTree: React.FC<ModelTreeProps> = ({ nodes, onSelectNode }) => {
  const groupedNodes = nodes.reduce((acc, node) => {
    const type = node.data.type || 'Other';
    if (!acc[type]) {
      acc[type] = [];
    }
    acc[type].push(node);
    return acc;
  }, {} as Record<string, Node[]>);

  return (
    <TreeView
      defaultCollapseIcon={<ExpandMore />}
      defaultExpandIcon={<ChevronRight />}
      sx={{ flexGrow: 1, maxWidth: 400, overflowY: 'auto' }}
    >
      {Object.entries(groupedNodes).map(([type, typeNodes]) => (
        <TreeItem key={type} nodeId={type} label={type} icon={<Folder />}>
          {typeNodes.map((node) => (
            <TreeItem
              key={node.id}
              nodeId={node.id}
              label={node.data.label}
              icon={<Description />}
              onClick={() => onSelectNode(node)}
            />
          ))}
        </TreeItem>
      ))}
    </TreeView>
  );
};

export default ModelTree;