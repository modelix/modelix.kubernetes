import {GitBranchStatusData} from "../../../api/gitConnectorApi.ts";

export interface BranchNode {
  namePart: string;
  children: BranchNode[];
  branchData?: GitBranchStatusData;
  depth: number;
}

/**
 * Build a tree of BranchNode from a flat list of branch names.
 * @param branches List of branch names, e.g. ["feature/foo", "hotfix/urgent"]
 * @returns A root node (with empty name) whose descendants form the tree.
 */
export function buildBranchTree(branches: GitBranchStatusData[]): BranchNode {
  const root: BranchNode = {namePart: "", children: [], depth: 0};

  for (const branch of branches) {
    const parts = (branch.name ?? "").split("/");
    let cursor = root;

    for (let i = 0; i < parts.length; i++) {
      const segment = parts[i];
      // find or create the child for this segment
      let child = cursor.children.find((n) => n.namePart === segment);
      if (!child) {
        child = {namePart: segment, children: [], depth: i + 1};
        cursor.children.push(child);
      }

      // if this is the last segment, attach the branch as a leaf node
      if (i === parts.length - 1) {
        child.branchData = branch;
      }

      // descend
      cursor = child;
    }
  }

  return root;
}