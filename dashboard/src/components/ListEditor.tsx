import {Component, ReactElement, ReactNode} from "react";
import Box from "@mui/material/Box";
import Collapse from "@mui/material/Collapse";
import Typography from "@mui/material/Typography";

interface ListEditorProps<E> {
  initialData: E[];
  onChange: (data: E[]) => void;
  itemComponent: (
    element: E,
    index: number,
    isNewItem: boolean,
    deleteItem: () => void,
    updateItem: (newValue: E) => void,
  ) => ReactNode;
  newItemTemplate?: E;
}

export default class ListEditor<E> extends Component<ListEditorProps<E>, {
  modifiedData: E[] | null,
}> {

  constructor(props: ListEditorProps<E>) {
    super(props);
    this.state = {
      modifiedData: null,
    };
  }

  render() {
    const list = this.state?.modifiedData ?? this.props.initialData;

    return (
      <>
        {list.map((element, index) =>
          this.props.itemComponent(
              element,
              index,
              false,
              () => this.props.onChange(list.filter((_, i) => i !== index)),
              (newValue) => this.props.onChange(list.map((e, i) => i === index ? newValue : e))
          )
        )}
        {this.props.newItemTemplate !== undefined && this.props.itemComponent(
            this.props.newItemTemplate,
            list.length,
            true,
            () => {},
            (newValue) => this.props.onChange(list.concat(newValue))
        )}
      </>
    );
  }
}
