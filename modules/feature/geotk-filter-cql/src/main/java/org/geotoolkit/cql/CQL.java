/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.cql;

import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.gui.swing.tree.Trees;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CQL {

    private CQL() {}
    
    private static Object compileInternal(String expression) {
        try {
            //lexer splits input into tokens
            final ANTLRStringStream input = new ANTLRStringStream(expression);
            final TokenStream tokens = new CommonTokenStream(new cqlLexer(input));

            //parser generates abstract syntax tree
            final cqlParser parser = new cqlParser(tokens);
            final cqlParser.expression_return retexp = parser.expression();
            final cqlParser.filter_return retfilter = parser.filter();
            
            if(retexp != null){
                return retexp;
            }else{
                return retfilter;
            }
            
        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }
    
    public static CommonTree compile(String expression) {
        final Object obj =compileInternal(expression);
        
        CommonTree tree = null;
        if(obj instanceof cqlParser.expression_return){
            tree = (CommonTree)((cqlParser.expression_return)obj).tree;
        }else if(obj instanceof cqlParser.filter_return){
            tree = (CommonTree)((cqlParser.filter_return)obj).tree;
        }
        
        return tree;
    }
    
    public static Object read(String cql) throws CQLException{
        final Object obj = compileInternal(cql);
        
        CommonTree tree = null;
        Object result = null;
        if(obj instanceof cqlParser.expression_return){
            tree = (CommonTree)((cqlParser.expression_return)obj).tree;
            result = convertExpression(tree, FactoryFinder.getFilterFactory(null));
        }else if(obj instanceof cqlParser.filter_return){
            tree = (CommonTree)((cqlParser.filter_return)obj).tree;
        }
        
        return result;
    }
    
    
    public static String write(Filter filter){
        final StringBuilder sb = new StringBuilder();
        filter.accept(FilterToCQLVisitor.INSTANCE,sb);
        return sb.toString();
    }
    
    public static String write(Expression exp){
        final StringBuilder sb = new StringBuilder();
        exp.accept(FilterToCQLVisitor.INSTANCE,sb);
        return sb.toString();
    }
    
    /**
     * Generate a nice looking tree representation of the tree.
     */
    public static String toString(CommonTree tree){
        if(tree == null) return null;        
        final DefaultMutableTreeNode node = explore(tree);
        return Trees.toString(node);
    }
    
    /**
     * Create a TreeNode for the given tree. method is recursive.
     */
    private static DefaultMutableTreeNode explore(CommonTree tree){
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);
        
        final List children = tree.getChildren();
        
        if(children != null){
            for(Object child : children){
                final DefaultMutableTreeNode n = explore((CommonTree)child);
                node.add(n);
            }
        }
        return node;
    }
    
    /**
     * Convert the given tree in a Filter or Expression.
     */
    private static Expression convertExpression(CommonTree tree, FilterFactory ff) throws CQLException{
        
        if(tree.token != null && tree.token.getTokenIndex() >= 0){
            final int type = tree.getType();
            if(cqlParser.PROPERTY_NAME_1 == type){
                //strip start and end "
                final String text = tree.getText();
                return ff.property(text.substring(1, text.length()-1));
            }else if(cqlParser.PROPERTY_NAME_2 == type){
                return ff.property(tree.getText());
            }else if(cqlParser.INT == type){
                return ff.literal(Integer.valueOf(tree.getText()));
            }else if(cqlParser.FLOAT == type){
                return ff.literal(Double.valueOf(tree.getText()));
            }else if(cqlParser.TEXT == type){
                //strip start and end '
                final String text = tree.getText();
                return ff.literal(text.substring(1, text.length()-1));
            }else if(cqlParser.OPERATOR == type){
                final String text = tree.getText();
                final Expression left = convertExpression((CommonTree)tree.getChild(0), ff);
                final Expression right = convertExpression((CommonTree)tree.getChild(1), ff);
                if("+".equals(text)){
                    return ff.add(left,right);
                }else if("-".equals(text)){
                    return ff.subtract(left,right);
                }else if("*".equals(text)){
                    return ff.multiply(left,right);
                }else if("/".equals(text)){
                    return ff.divide(left,right);
                }
            }else if(cqlParser.FUNCTION_NAME == type){
                String functionName = tree.getText();
                //remove the (
                functionName = functionName.substring(0, functionName.length()-1);
                final List<Expression> exps = new ArrayList<Expression>();
                for(Object child : tree.getChildren()){
                    exps.add(convertExpression((CommonTree)child, ff));
                }
                return ff.function(functionName, exps.toArray(new Expression[exps.size()]));
            }
        }
        
        throw new CQLException("Unreconized expression : type="+tree.getType()+" text=" + tree.getText());
    }
    
}
