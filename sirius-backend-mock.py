#!/usr/bin/env python3
"""
Sirius Web后端模拟服务 - 提供必要的GraphQL端点
"""
from flask import Flask, jsonify, request
from flask_cors import CORS
from flask_graphql import GraphQLView
from graphql import GraphQLSchema, GraphQLObjectType, GraphQLField, GraphQLString, GraphQLList, GraphQLNonNull, GraphQLID, GraphQLArgument
import json

app = Flask(__name__)
CORS(app, origins=["http://localhost:5173"])

# 模拟数据
projects = [
    {"id": "1", "name": "SysML Demo Project", "description": "演示项目"},
    {"id": "2", "name": "Aircraft Fuel System", "description": "飞机燃油系统模型"}
]

representations = [
    {"id": "r1", "name": "Requirements Diagram", "type": "Diagram"},
    {"id": "r2", "name": "Block Definition Diagram", "type": "Diagram"}
]

# GraphQL Schema
ProjectType = GraphQLObjectType(
    "Project",
    lambda: {
        "id": GraphQLField(GraphQLNonNull(GraphQLID)),
        "name": GraphQLField(GraphQLString),
        "description": GraphQLField(GraphQLString),
    }
)

RepresentationType = GraphQLObjectType(
    "Representation",
    lambda: {
        "id": GraphQLField(GraphQLNonNull(GraphQLID)),
        "name": GraphQLField(GraphQLString),
        "type": GraphQLField(GraphQLString),
    }
)

ViewerType = GraphQLObjectType(
    "Viewer",
    lambda: {
        "id": GraphQLField(GraphQLNonNull(GraphQLID)),
        "username": GraphQLField(GraphQLString),
        "projects": GraphQLField(GraphQLList(ProjectType)),
    }
)

QueryType = GraphQLObjectType(
    "Query",
    lambda: {
        "viewer": GraphQLField(
            ViewerType,
            resolver=lambda obj, info: {
                "id": "viewer-1",
                "username": "demo-user",
                "projects": projects
            }
        ),
        "project": GraphQLField(
            ProjectType,
            args={"id": GraphQLArgument(GraphQLNonNull(GraphQLID))},
            resolver=lambda obj, info, id: next((p for p in projects if p["id"] == id), None)
        ),
    }
)

MutationType = GraphQLObjectType(
    "Mutation",
    lambda: {
        "createProject": GraphQLField(
            ProjectType,
            args={
                "name": GraphQLArgument(GraphQLNonNull(GraphQLString)),
                "description": GraphQLArgument(GraphQLString)
            },
            resolver=lambda obj, info, name, description=None: {
                "id": str(len(projects) + 1),
                "name": name,
                "description": description
            }
        ),
    }
)

schema = GraphQLSchema(query=QueryType, mutation=MutationType)

# REST端点
@app.route('/api/projects', methods=['GET'])
def get_projects():
    return jsonify(projects)

@app.route('/api/representations', methods=['GET'])
def get_representations():
    return jsonify(representations)

@app.route('/api/capabilities', methods=['GET'])
def get_capabilities():
    return jsonify({
        "siriusWeb": True,
        "version": "2024.11.0",
        "features": ["diagrams", "forms", "tables", "trees"]
    })

# 健康检查
@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "UP",
        "components": {
            "graphql": {"status": "UP"},
            "database": {"status": "UP"},
            "cdo": {"status": "UP", "repository": "sysml"}
        }
    })

# GraphQL端点
app.add_url_rule(
    '/graphql',
    view_func=GraphQLView.as_view('graphql', schema=schema, graphiql=True)
)

# WebSocket模拟（简化版）
@app.route('/subscriptions', methods=['GET', 'POST'])
def subscriptions():
    return jsonify({"message": "WebSocket endpoint"})

if __name__ == '__main__':
    print("=" * 50)
    print("Sirius Web后端服务启动")
    print("=" * 50)
    print("GraphQL端点: http://localhost:8080/graphql")
    print("健康检查: http://localhost:8080/health")
    print("=" * 50)
    app.run(host='0.0.0.0', port=8080, debug=True)