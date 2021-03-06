package com.jetbrains.jetpad.vclang.typechecking.termination;

/*Generated by MPS */

class TestCallMatrix extends BaseCallMatrix<TestVertex> {
    private TestVertex myDomain;
    private TestVertex myCodomain;
    private String myLabel;

    TestCallMatrix(String label, TestVertex dom, TestVertex codom, Object... data) {
        super(codom.myArguments.length, dom.myArguments.length);
        myCodomain = codom;
        myDomain = dom;
        myLabel = label;
        int pos = 0;
        int i = 0;
        while (pos < data.length) {
            Object entity = data[pos];
            if (entity instanceof Character) {
                char c = (char) entity;
                if (c == '?') {
                    i++;
                    pos++;
                } else if ((c == '<' || c == '=') && pos < data.length - 1) {
                    Object entity2 = data[pos + 1];
                    if (entity2 instanceof Integer) {
                        Integer j = (Integer) entity2;
                        BaseCallMatrix.R r = (c == '<' ? BaseCallMatrix.R.LessThan : BaseCallMatrix.R.Equal);
                        set(i, j, r);
                        i++;
                        pos += 2;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException("There is a problem with argument #" + pos);
            }
        }
    }

    @Override
    public TestVertex getCodomain() {
        return myCodomain;
    }

    @Override
    public TestVertex getDomain() {
        return myDomain;
    }

    @Override
    public int getCompositeLength() {
        return 1;
    }

    @Override
    protected String[] getColumnLabels() {
        return myCodomain.myArguments;
    }

    @Override
    protected String[] getRowLabels() {
        return myDomain.myArguments;
    }

    @Override
    public String getMatrixLabel() {
        return myLabel;
    }

}
